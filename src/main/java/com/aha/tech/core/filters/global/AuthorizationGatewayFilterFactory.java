package com.aha.tech.core.filters.global;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.constant.GatewayFilterProcessOrderedConstant;
import com.aha.tech.core.exception.GatewayException;
import com.aha.tech.core.service.RequestHandlerService;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
        return GatewayFilterProcessOrderedConstant.GLOBAL_AUTH_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始执行权限校验网关过滤器");


        String path = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        ServerHttpRequest newRequest;
        try {
            newRequest = httpRequestHandlerService.authorize(exchange);
        } catch (GatewayException ge) {
            return Mono.defer(() -> writeWithGatewayError(exchange, path, ge));
        } catch (Exception e) {
            return Mono.defer(() -> writeWithError(exchange, e));
        }

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    /**
     * 根据错误code码返回信息
     * @param exchange
     * @param e
     * @return
     */
    private Mono<Void> writeWithGatewayError(ServerWebExchange exchange, String path, GatewayException e) {
        logger.warn("访问路径: {} 失败,原因 : 权限不足", path, e);
        setResponseStatus(exchange, HttpStatus.UNAUTHORIZED);
        final ServerHttpResponse resp = exchange.getResponse();
        RpcResponse rpcResponse = new RpcResponse(e.getCode(), e.getMessage());
        byte[] bytes = JSON.toJSONString(rpcResponse).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
        resp.getHeaders().setContentLength(buffer.readableByteCount());
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        return resp.writeWith(Flux.just(buffer));
    }

    /**
     * 非业务异常捕获
     * @param exchange
     * @param e
     * @return
     */
    private Mono<Void> writeWithError(ServerWebExchange exchange, Exception e) {
        logger.error("权限校验过滤器出现异常", e);
        setResponseStatus(exchange, HttpStatus.BAD_GATEWAY);
        final ServerHttpResponse resp = exchange.getResponse();
        RpcResponse rpcResponse = RpcResponse.defaultFailureResponse();
        String message = e.getMessage();
        if (StringUtils.isBlank(message) && e.getCause() != null) {
            message = e.getCause().toString();
        } else {
            message = e.getClass().toString();
        }

        rpcResponse.setMessage(message);
        byte[] bytes = JSON.toJSONString(rpcResponse).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
        resp.getHeaders().setContentLength(buffer.readableByteCount());
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        return resp.writeWith(Flux.just(buffer));
    }

}
