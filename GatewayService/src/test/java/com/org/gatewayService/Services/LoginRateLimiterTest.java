package com.org.gatewayService.Services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Verifies the per-IP fixed-window budget: each IP gets its own allowance,
 * login and admin-login budgets are independent, and one exhausted IP does
 * not affect another (the DoS fix vs the old global bucket).
 */
class LoginRateLimiterTest {

	// window long enough that it won't roll over mid-test
	private LoginRateLimiter limiter(int loginLimit, int adminLimit) {
		return new LoginRateLimiter(300, loginLimit, adminLimit);
	}

	@Test
	void allowsUpToTheLimitThenBlocksForThatIp() {
		LoginRateLimiter rl = limiter(3, 2);
		String ip = "203.0.113.7";

		assertThat(rl.allowLogin(ip)).isTrue();   // 1
		assertThat(rl.allowLogin(ip)).isTrue();   // 2
		assertThat(rl.allowLogin(ip)).isTrue();   // 3
		assertThat(rl.allowLogin(ip)).isFalse();  // 4 — over limit
		assertThat(rl.allowLogin(ip)).isFalse();  // stays blocked
	}

	@Test
	void oneExhaustedIpDoesNotAffectAnother() {
		LoginRateLimiter rl = limiter(2, 2);

		assertThat(rl.allowLogin("10.0.0.1")).isTrue();
		assertThat(rl.allowLogin("10.0.0.1")).isTrue();
		assertThat(rl.allowLogin("10.0.0.1")).isFalse(); // first IP exhausted

		// a different IP still has its full budget — the whole point of keying
		assertThat(rl.allowLogin("10.0.0.2")).isTrue();
		assertThat(rl.allowLogin("10.0.0.2")).isTrue();
		assertThat(rl.allowLogin("10.0.0.2")).isFalse();
	}

	@Test
	void loginAndAdminBudgetsAreIndependentForTheSameIp() {
		LoginRateLimiter rl = limiter(3, 1);
		String ip = "198.51.100.9";

		// exhaust the admin budget (limit 1)
		assertThat(rl.allowAdminLogin(ip)).isTrue();
		assertThat(rl.allowAdminLogin(ip)).isFalse();

		// regular login from the same IP is unaffected (separate key prefix)
		assertThat(rl.allowLogin(ip)).isTrue();
		assertThat(rl.allowLogin(ip)).isTrue();
		assertThat(rl.allowLogin(ip)).isTrue();
		assertThat(rl.allowLogin(ip)).isFalse();
	}
}
