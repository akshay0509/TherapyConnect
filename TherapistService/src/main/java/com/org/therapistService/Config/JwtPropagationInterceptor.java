package com.org.therapistService.Config;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import com.org.therapistService.Utility.SecurityUtils;

public class JwtPropagationInterceptor implements ClientHttpRequestInterceptor{

	@Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        String jwt = SecurityUtils.getJwtToken();

        request.getHeaders()
                .setBearerAuth(jwt);

        return execution.execute(request, body);
    }
}
