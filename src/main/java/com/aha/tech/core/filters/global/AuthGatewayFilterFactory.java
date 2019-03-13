package com.aha.tech.core.filters.global;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.constant.FilterOrderedConstant;
import com.aha.tech.core.controller.resource.PassportResource;
import com.aha.tech.core.exception.AnonymousUserException;
import com.aha.tech.core.exception.AuthorizationFailedException;
import com.aha.tech.core.exception.MissAuthorizationHeaderException;
import com.aha.tech.core.model.entity.AuthenticationEntity;
import com.aha.tech.core.model.entity.PairEntity;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.aha.tech.commons.constants.ResponseConstants.SUCCESS;
import static com.aha.tech.core.constant.ExchangeAttributeConstant.URL_IN_WHITE_LIST;
import static com.aha.tech.core.constant.ExchangeAttributeConstant.USER_INFO_SESSION;
import static com.aha.tech.core.constant.HeaderFieldConstant.DEFAULT_X_TOKEN_VALUE;
import static com.aha.tech.core.constant.HeaderFieldConstant.HEADER_AUTHORIZATION;

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

    private static final String NEED_AUTHORIZATION = "serv-auth";

    @Autowired(required = false)
    private PassportResource passportResource;

    @Override
    public int getOrder() {
        return FilterOrderedConstant.GLOBAL_AUTH_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行授权auth 过滤器");

        Boolean verifyResult = verifyPermission(exchange);

        if (!verifyResult) {
            throw new AuthorizationFailedException();
        }

        return chain.filter(exchange);
    }

    /**
     * 权限校验
     * @param exchange
     * @return
     */
    private boolean verifyPermission(ServerWebExchange exchange) {
        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
        Boolean isWhiteList = (Boolean) exchange.getAttributes().get(URL_IN_WHITE_LIST);

        PairEntity<String> authorization = parseAuthorizationHeader(requestHeaders);
        String userName = authorization.getFirstEntity();
        String password = authorization.getSecondEntity();

        if (userName.equals(VISITOR)) {
            AuthenticationEntity authenticationEntity = verifyVisitorPermission(password, isWhiteList);
            Boolean verifyResult = authenticationEntity.getVerifyResult();
            if (!verifyResult) {
                throw new AnonymousUserException();
            }

            exchange.getAttributes().put(USER_INFO_SESSION, UserVo.anonymousUser());
            return Boolean.TRUE;
        }

        if (userName.equals(NEED_AUTHORIZATION)) {
            logger.debug("access token is : {}", password);
            AuthenticationEntity authenticationEntity = verifyAccessTokenPermission(password);
            Boolean verifyResult = authenticationEntity.getVerifyResult();
            RpcResponse<UserVo> rpcResponse = authenticationEntity.getRpcResponse();
            if (!verifyResult) {
                throw new AuthorizationFailedException(rpcResponse.getCode(), rpcResponse.getMessage());
            }

            exchange.getAttributes().put(USER_INFO_SESSION, rpcResponse.getData());
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    private AuthenticationEntity verifyAccessTokenPermission(String accessToken) {
        AuthenticationEntity authenticationEntity = new AuthenticationEntity();
        RpcResponse<UserVo> authResult = passportResource.verify(accessToken);
        int code = authResult.getCode();
        authenticationEntity.setVerifyResult(code == SUCCESS);
        authenticationEntity.setRpcResponse(authResult);

        return authenticationEntity;
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

        return new PairEntity(arr[0], arr[1]);
    }

    /**
     * 如果是访客 则校验默认令牌
     *
     * @param defaultToken
     * @param isWhite
     * @return
     */
    private AuthenticationEntity verifyVisitorPermission(String defaultToken, Boolean isWhite) {
        AuthenticationEntity authenticationEntity = new AuthenticationEntity();
        authenticationEntity.setVerifyResult(defaultToken.equals(DEFAULT_X_TOKEN_VALUE) && isWhite);

        return authenticationEntity;
    }

}
