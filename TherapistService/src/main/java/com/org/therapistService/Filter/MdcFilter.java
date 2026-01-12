package com.org.therapistService.Filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class MdcFilter extends OncePerRequestFilter{

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		try {
			// 1. Generate or extract a Correlation ID (Tracing)
			String traceId = request.getHeader("X-Correlation-ID");
			if (traceId == null) {
				traceId = UUID.randomUUID().toString();
			}

			// 2. Add to MDC (The keys must match your log4j2.xml layout)
			MDC.put("traceId", traceId);
			MDC.put("path", request.getRequestURI());

			// 3. Optional: Add Therapist ID if it exists in the header
			String therapistId = request.getHeader("X-Therapist-ID");
			if (therapistId != null) {
				MDC.put("therapistId", therapistId);
			}
			else {
				MDC.put("therapistId", "ANONYMOUS");
			}

			filterChain.doFilter(request, response);
		} finally {
			// 4. CRITICAL: Always clear MDC at the end of the thread's work
			MDC.clear();
		}
	}
}
