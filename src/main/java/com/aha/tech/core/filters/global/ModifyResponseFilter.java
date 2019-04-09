package com.aha.tech.core.filters.global;

import com.aha.tech.core.service.RequestHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_RESPONSE_GATEWAY_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/4/1
 */
@Component
public class ModifyResponseFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyResponseFilter.class);

    @Resource
    private ThreadPoolTaskExecutor printAccessLogThreadPool;

    @Override
    public int getOrder() {
        return MODIFY_RESPONSE_GATEWAY_FILTER_ORDER;
    }

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始修改返回值过滤器");

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            httpRequestHandlerService.modifyResponseHeader(response.getHeaders());
            CompletableFuture.runAsync(() -> httpRequestHandlerService.writeResultInfo(exchange), printAccessLogThreadPool);
        }));
    }

}
