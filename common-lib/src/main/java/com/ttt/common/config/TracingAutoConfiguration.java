package com.ttt.common.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class TracingAutoConfiguration {

    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String MDC_TRACE_ID_KEY = "traceId";

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public Filter servletTraceFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request,
                    @NonNull HttpServletResponse response,
                    @NonNull FilterChain filterChain) throws ServletException, IOException {
                String traceId = request.getHeader(TRACE_ID_HEADER);
                if (traceId == null) {
                    traceId = UUID.randomUUID().toString().substring(0, 8);
                    request.setAttribute(TRACE_ID_HEADER, traceId);
                }

                try {
                    MDC.put(MDC_TRACE_ID_KEY, traceId);
                    log.debug("IN  {} {}", request.getMethod(), request.getRequestURI());
                    filterChain.doFilter(request, response);
                    log.debug("OUT status: {}", response.getStatus());
                } finally {
                    MDC.remove(MDC_TRACE_ID_KEY);
                }
            }
        };
    }
}
