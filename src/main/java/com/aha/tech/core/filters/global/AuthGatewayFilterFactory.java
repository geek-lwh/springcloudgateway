package com.aha.tech.core.filters.global;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.constant.FilterOrderedConstant;
import com.aha.tech.core.controller.resource.PassportResource;
import com.aha.tech.core.exception.MissAuthorizationHeaderException;
import com.aha.tech.core.handler.SessionHandler;
import com.aha.tech.core.model.entity.PairEntity;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.binary.Base64;
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
import static com.aha.tech.core.constant.HeaderFieldConstant.DEFAULT_X_TOKEN_VALUE;
import static com.aha.tech.core.constant.HeaderFieldConstant.HEADER_AUTHORIZATION;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/2/14
 *
 * 修改requestBody的同时,需要修改header的content length
 * 否则增加的字节会被截断,导致后端服务报json解析不正确
 */
@Component
public class AuthGatewayFilterFactory implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthGatewayFilterFactory.class);

    private static final String VISITOR = "visitor";

    private static final String USER = "serv-auth";

    @Autowired(required = false)
    private PassportResource passportResource;

    @Override
    public int getOrder() {
        return FilterOrderedConstant.GLOBAL_AUTH_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行授权auth 过滤器");
        UserVo userVo = null;
        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
        PairEntity<String> authorization = parseAuthorizationHeader(requestHeaders);
        String userName = authorization.getFirstEntity();
        String password = authorization.getSecondEntity();

        if (userName.equals(USER)) {
            logger.debug("access token is : {}", password);
            RpcResponse<UserVo> authResult = passportResource.verify(password);
            if (authResult.getCode() != SUCCESS) {
                return Mono.defer(() -> {
                    setResponseStatus(exchange, HttpStatus.UNAUTHORIZED);
                    final ServerHttpResponse resp = exchange.getResponse();
                    byte[] bytes = JSON.toJSONString(authResult).getBytes(StandardCharsets.UTF_8);
                    DataBuffer buffer = resp.bufferFactory().wrap(bytes);
                    return resp.writeWith(Flux.just(buffer));
                });
            }
            userVo = authResult.getData();
        } else if (userName.equals(VISITOR) && password.equals(DEFAULT_X_TOKEN_VALUE)) {
            logger.info("设置匿名用户 userId = 0");
            userVo.setUserId(0L);
        }

        if(userVo == null){
            logger.error("user is empty,check your access token");
        }
        SessionHandler.set(userVo);

        return chain.filter(exchange);
    }

    /**
     * 解析http header 中的Authorization
     *
     * @param requestHeaders
     * @return
     */
    private PairEntity parseAuthorizationHeader(HttpHeaders requestHeaders) {
        List<String> headersOfAuthorization = requestHeaders.get(HEADER_AUTHORIZATION);
        if (CollectionUtils.isEmpty(headersOfAuthorization)) {
            throw new MissAuthorizationHeaderException();
        }

        String authorizationHeader = headersOfAuthorization.get(0).substring(6);
        String decodeAuthorization = new String(Base64.decodeBase64(authorizationHeader), StandardCharsets.UTF_8);
        String[] arr = decodeAuthorization.split(":");

        return new PairEntity(arr[0],arr[1]);
    }

}
