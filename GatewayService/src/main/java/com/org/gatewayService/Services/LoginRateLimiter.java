package com.org.gatewayService.Services;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Per-client-IP fixed-window login throttle.
 *
 * Replaces the previous Resilience4j @RateLimiter, which used a single global
 * bucket (10 attempts/min for the WHOLE platform) — so one noisy source could
 * exhaust it and lock every user out of login, a trivial DoS. Keying by IP
 * gives each source its own budget. Per-account brute force is separately
 * bounded by the existing failed-attempt account lockout in UserService, which
 * is why the key is the IP alone: keying by IP+username would instead let a
 * single IP multiply its throughput by rotating usernames.
 *
 * Fixed-window, not token-bucket: one counter per IP that expires
 * windowSeconds after its first request in the window. In-memory and
 * Caffeine-evicted, which is correct for the single-instance gateway. If the
 * gateway is ever scaled to multiple replicas, move this to a shared store
 * (e.g. Redis) so the window is enforced globally rather than per-instance.
 *
 * The IP comes from ClientIpUtil (nginx X-Real-IP) and is trustworthy only
 * because the gateway port is not directly reachable from the internet.
 */
@Component
public class LoginRateLimiter {

	private final Cache<String, AtomicInteger> counters;
	private final int loginLimit;
	private final int adminLoginLimit;

	public LoginRateLimiter(
			@Value("${security.login.rate-limit.window-seconds:60}") long windowSeconds,
			@Value("${security.login.rate-limit.per-ip:10}") int loginLimit,
			@Value("${security.admin-login.rate-limit.per-ip:5}") int adminLoginLimit) {

		this.loginLimit = loginLimit;
		this.adminLoginLimit = adminLoginLimit;
		this.counters = Caffeine.newBuilder()
				.expireAfterWrite(Duration.ofSeconds(windowSeconds))
				.maximumSize(100_000)
				.build();
	}

	public boolean allowLogin(String clientIp) {
		return tryAcquire("login:" + clientIp, loginLimit);
	}

	public boolean allowAdminLogin(String clientIp) {
		return tryAcquire("admin:" + clientIp, adminLoginLimit);
	}

	private boolean tryAcquire(String key, int limit) {
		// get(key, fn) creates the counter atomically on the first request of a
		// window; expireAfterWrite means incrementing an existing entry does
		// not extend the window, so it stays a true fixed window.
		AtomicInteger counter = counters.get(key, k -> new AtomicInteger());
		return counter.incrementAndGet() <= limit;
	}
}
