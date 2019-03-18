package com.aha.tech.core.filters.global;

import com.aha.tech.core.service.ModifyResponseService;
import com.aha.tech.core.service.RequestHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.FilterOrderedConstant.GLOBAL_MODIFY_RESPONSE_HEADER_GATEWAY_FILTER_ORDER;
import static com.aha.tech.core.constant.HeaderFieldConstant.*;

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 * 修改response header 网关过滤器
 */
@Component
public class ModifyResponseHeaderGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyResponseHeaderGatewayFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return GLOBAL_MODIFY_RESPONSE_HEADER_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行修改返回体报头信息网关过滤器");

        HttpHeaders httpHeaders = exchange.getResponse().getHeaders();
        httpRequestHandlerService.modifyResponseHeaders(exchange);

        return chain.filter(exchange);
    }

}