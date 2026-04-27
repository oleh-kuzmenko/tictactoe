package com.ttt.common.interceptor;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

import com.ttt.common.config.TracingAutoConfiguration;

import java.io.IOException;

public class TraceIdRestClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public @NonNull ClientHttpResponse intercept(@NonNull HttpRequest request,
                                                 byte[] body,
                                                 ClientHttpRequestExecution execution) throws IOException {

        String traceId = MDC.get(TracingAutoConfiguration.MDC_TRACE_ID_KEY);

        if (traceId != null) {
            request.getHeaders().add(TracingAutoConfiguration.TRACE_ID_HEADER, traceId);
        }

        return execution.execute(request, body);
    }
}
