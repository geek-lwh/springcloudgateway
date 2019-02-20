package com.aha.tech.core.filters.global;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.constant.FilterOrdered;
import com.aha.tech.core.controller.resource.PassportResource;
import com.aha.tech.core.handler.SessionHandler;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.aha.tech.commons.constants.ResponseConstants.SUCCESS;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/2/14
 *
 * need to modify the header Content-Length, If you don't do this, the body may be truncated after you have modify the request body and the body becomes longer
 */
@Component
public class AuthGatewayFilterFactory implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthGatewayFilterFactory.class);

    private static final String ACCESS_TOKEN_HEADER = "token";

    @Autowired(required = false)
    private PassportResource passportResource;

    @Override
    public int getOrder() {
        return FilterOrdered.GLOBAL_AUTH_GATEWAY_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行授权auth 过滤器");
        if (isWhiteList()) {
            logger.info("白名单资源,不进行授权行为, uri : {} ", exchange.getRequest().getURI());
            return chain.filter(exchange);
        }

        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
        List<String> headersOfToken = requestHeaders.get(ACCESS_TOKEN_HEADER);
        String accessToken = CollectionUtils.isEmpty(headersOfToken) ? StringUtils.EMPTY : headersOfToken.get(0);

        // 请求授权系统 验证access token 是否合法
        RpcResponse<UserVo> authResult = passportResource.verify(accessToken);
        if (authResult.getCode() != SUCCESS) {
            return Mono.defer(() -> {
                setResponseStatus(exchange, HttpStatus.UNAUTHORIZED);
                final ServerHttpResponse resp = exchange.getResponse();
                byte[] bytes = JSON.toJSONString(authResult).getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = resp.bufferFactory().wrap(bytes);
                return resp.writeWith(Flux.just(buffer));
            });
        }

        UserVo userVo = authResult.getData();
        SessionHandler.set(userVo);
        return chain.filter(exchange);
    }

    /**
     * 白名单不用授权
     * @return
     */
    private static Boolean isWhiteList() {
        return Boolean.FALSE;
    }

}
