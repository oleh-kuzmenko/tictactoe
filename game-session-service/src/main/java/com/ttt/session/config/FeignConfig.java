package com.ttt.session.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ttt.common.config.TracingAutoConfiguration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor traceIdInterceptor() {
        return template -> {
            String traceId = MDC.get(TracingAutoConfiguration.MDC_TRACE_ID_KEY);
            if (traceId != null) {
                template.header(TracingAutoConfiguration.TRACE_ID_HEADER, traceId);
            }
        };
    }
}
