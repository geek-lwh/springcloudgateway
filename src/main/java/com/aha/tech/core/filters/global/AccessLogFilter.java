package com.aha.tech.core.filters.global;

import com.aha.tech.core.service.RequestHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.ACCESS_LOG_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/4/8
 */
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AccessLogFilter.class);

    @Resource
    private ThreadPoolTaskExecutor printAccessLogThreadPool;

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return ACCESS_LOG_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        CompletableFuture.runAsync(() -> httpRequestHandlerService.writeAccessInfo(exchange), printAccessLogThreadPool);
        return chain.filter(exchange);
    }
}
