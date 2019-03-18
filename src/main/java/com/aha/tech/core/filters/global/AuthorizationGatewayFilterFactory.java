package com.aha.tech.core.filters.global;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.constant.FilterOrderedConstant;
import com.aha.tech.core.service.RequestHandlerService;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/2/14
 *
 * 鉴权校验
 */
@Component
public class AuthorizationGatewayFilterFactory implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationGatewayFilterFactory.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;


    @Override
    public int getOrder() {
        return FilterOrderedConstant.GLOBAL_AUTH_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行授权auth 过滤器");

        RpcResponse rpcResponse = new RpcResponse();
        String path = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        ServerHttpRequest newRequest;
        try {
            newRequest = httpRequestHandlerService.authorize(exchange);
        } catch (Exception e) {
            return Mono.defer(() -> {
                logger.warn("访问路径: {} 失败,原因 : 权限不足", path, e);
                setResponseStatus(exchange, HttpStatus.UNAUTHORIZED);
                final ServerHttpResponse resp = exchange.getResponse();
                byte[] bytes = JSON.toJSONString(rpcResponse).getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = resp.bufferFactory().wrap(bytes);
                return resp.writeWith(Flux.just(buffer));
            });
        }

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

}
