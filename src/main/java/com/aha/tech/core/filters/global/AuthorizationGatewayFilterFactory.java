package com.aha.tech.core.filters.global;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.constant.FilterOrderedConstant;
import com.aha.tech.core.exception.AuthorizationFailedException;
import com.aha.tech.core.service.RequestHandlerService;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

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
        Boolean verifyResult = Boolean.FALSE;
        try {
            verifyResult = httpRequestHandlerService.authorize(exchange);
        } catch (AuthorizationFailedException e) {
            rpcResponse.setCode(e.getCode());
            rpcResponse.setMessage(e.getMessage());
        }

        // 如果校验不通过则直接返回
        if (!verifyResult) {
            return Mono.defer(() -> {
                setResponseStatus(exchange, HttpStatus.UNAUTHORIZED);
                final ServerHttpResponse resp = exchange.getResponse();
                byte[] bytes = JSON.toJSONString(rpcResponse).getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = resp.bufferFactory().wrap(bytes);
                return resp.writeWith(Flux.just(buffer));
            });
        }

        //todo addbody 或者 addParams
//        HttpMethod httpMethod = exchange.getRequest().getMethod();
//        httpRequestHandlerService.
//        ServerHttpRequest newRequest =
        return chain.filter(exchange);
    }

}
