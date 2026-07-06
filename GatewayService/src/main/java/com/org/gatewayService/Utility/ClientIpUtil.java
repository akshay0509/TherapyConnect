package com.org.gatewayService.Utility;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the real client IP behind the nginx reverse proxy.
 *
 * nginx sets X-Real-IP to its own $remote_addr — the browser's actual TCP
 * peer address — and overwrites any client-supplied value, so it cannot be
 * forged via a request header. We deliberately read X-Real-IP rather than
 * X-Forwarded-For: nginx builds XFF with $proxy_add_x_forwarded_for, which
 * appends to (and trusts) a client-supplied XFF, making its left-most entry
 * spoofable.
 *
 * Falls back to getRemoteAddr() when the header is absent (local dev with no
 * proxy in front).
 *
 * SECURITY PRECONDITION: this is only trustworthy while the gateway port
 * (8091) is NOT directly reachable from the internet — only nginx (443) must
 * be public. If 8091 is exposed, an attacker can bypass nginx and set any
 * X-Real-IP. Keep the Oracle security list / ufw restricted to 443 + 22.
 */
public final class ClientIpUtil {

	private ClientIpUtil() {
	}

	public static String resolve(HttpServletRequest request) {
		String realIp = request.getHeader("X-Real-IP");
		if (realIp != null && !realIp.isBlank()) {
			return realIp.trim();
		}
		return request.getRemoteAddr();
	}
}
