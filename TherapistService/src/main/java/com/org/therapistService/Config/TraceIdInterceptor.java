package com.org.therapistService.Config;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class TraceIdInterceptor implements ClientHttpRequestInterceptor{

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		// 1. Get the ID from the current thread's context
        String currentTraceId = MDC.get("traceId");

        // 2. If it exists, inject it into the outgoing header
        if (currentTraceId != null) {
            // Use the same header name your Filter looks for ("X-Correlation-ID")
            request.getHeaders().add("X-Correlation-ID", currentTraceId);
        }

        // 3. Proceed with the request
        return execution.execute(request, body);
    }

}
