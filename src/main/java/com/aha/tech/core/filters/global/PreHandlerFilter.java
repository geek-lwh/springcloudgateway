package com.aha.tech.core.filters.global;

import com.aha.tech.core.service.ModifyResponseService;
import com.aha.tech.core.service.RequestHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.PRE_HANDLER_FILTER_ORDER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/4/8
 *
 * 预处理过滤器
 */
@Component
public class PreHandlerFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(PreHandlerFilter.class);

    @Resource
    private ThreadPoolTaskExecutor printAccessLogThreadPool;

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Resource
    private ModifyResponseService httpModifyResponseService;

    @Override
    public int getOrder() {
        return PRE_HANDLER_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        CompletableFuture.runAsync(() -> httpRequestHandlerService.writeAccessInfo(exchange), printAccessLogThreadPool);

        HttpMethod httpMethod = exchange.getRequest().getMethod();
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders httpHeaders = response.getHeaders();
//        httpModifyResponseService.crossAccessSetting(httpHeaders);
        if (httpMethod.equals(HttpMethod.OPTIONS)) {
            setResponseStatus(exchange, HttpStatus.OK);
            return Mono.defer(() -> response.writeWith(Mono.empty()));
        }

        return chain.filter(exchange);
    }
}
