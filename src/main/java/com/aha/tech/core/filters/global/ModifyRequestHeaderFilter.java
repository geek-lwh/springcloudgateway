package com.aha.tech.core.filters.global;

import com.aha.tech.core.exception.GatewayException;
import com.aha.tech.core.service.RequestHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER;
import static com.aha.tech.core.support.WriteResponseSupport.writeError;

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 *
 * 修改请求头网关过滤器
 */
@Component
public class ModifyRequestHeaderFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyRequestHeaderFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始进行修改请求头网关过滤器");
        ServerHttpRequest newRequest;
        try {
            ServerHttpRequest serverHttpRequest = exchange.getRequest();
            HttpHeaders httpHeaders = serverHttpRequest.getHeaders();

            httpHeaders.forEach((key, value) -> logger.debug("原始报头信息 key : {},value : {}", key, value));
            HttpHeaders newHttpHeaders = httpRequestHandlerService.modifyRequestHeaders(httpHeaders);
            newHttpHeaders.forEach((key, value) -> logger.debug("新的报头信息 key : {},value : {}", key, value));
            newRequest = new ServerHttpRequestDecorator(serverHttpRequest) {
                @Override
                public HttpHeaders getHeaders() {
                    return newHttpHeaders;
                }
            };
        } catch (GatewayException e) {
            return Mono.defer(() -> writeError(exchange, e));
        }

        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
        return chain.filter(newExchange);
    }

}
