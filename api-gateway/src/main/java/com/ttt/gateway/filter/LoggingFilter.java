package com.ttt.gateway.filter;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-ID";

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }

        log.debug("[{}] IN  {} {}", traceId, exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath());

        String finalId = traceId;
        return chain.filter(exchange.mutate()
                .request(r -> r.header(TRACE_ID_HEADER, finalId))
                .build())
                .doFinally(signal -> {
                    log.debug("[{}] OUT status: {}", finalId, exchange.getResponse().getStatusCode());
                });
    }
}
