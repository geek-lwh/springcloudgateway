package com.aha.tech.core.service.impl;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.exception.*;
import com.aha.tech.core.model.dto.Params;
import com.aha.tech.core.model.entity.AuthenticationEntity;
import com.aha.tech.core.model.entity.PairEntity;
import com.aha.tech.core.model.entity.RouteEntity;
import com.aha.tech.core.service.*;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import javax.annotation.Resource;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.ROUTE_ID;
import static com.aha.tech.core.constant.HeaderFieldConstant.HEADER_AUTHORIZATION;
import static com.aha.tech.core.tools.BeanUtil.copyMultiValueMap;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 *
 * http请求处理实现类
 */
@Service(value = "httpRequestHandlerService")
public class HttpRequestHandlerServiceImpl implements RequestHandlerService {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandlerServiceImpl.class);

    private static final int SKIP_STRIP_PREFIX_PART = 1;

    private static final String VISITOR = "visitor";

    private static final String NEED_AUTHORIZATION = "serv-auth";

    @Resource
    private RewritePathService httpRewritePathService;

    @Resource
    private AuthorizationService httpAuthorizationService;

    @Resource
    private ModifyHeaderService httpModifyHeaderService;

    @Resource
    private ModifyResponseService httpModifyResponseService;

    /**
     * 重写请求路径
     * @param serverWebExchange
     * @return
     */
    @Override
    public ServerHttpRequest rewriteRequestPath(ServerWebExchange serverWebExchange) {
        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
        URI uri = serverHttpRequest.getURI();
        String oldPath = uri.getRawPath();
        logger.info("开始重写请求路径,原路由路径 : {}", oldPath);

        // 去除路径中无效的字符
        String validPath = httpRewritePathService.excludeInvalidPath(oldPath, SKIP_STRIP_PREFIX_PART);

        // 重写请求路径
        RouteEntity routeEntity = httpRewritePathService.rewritePath(validPath);
        String rewritePath = routeEntity.getRewritePath();
        String id = routeEntity.getId();
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, rewritePath);
        serverWebExchange.getAttributes().put(ROUTE_ID, id);
        // 生成新的request对象
        ServerHttpRequest newRequest = serverHttpRequest.mutate()
                .path(rewritePath)
                .build();

        logger.info("结束重写请求路径,原路由路径 : {} , 新路由路径", rewritePath);

        return newRequest;
    }

    /**
     * 鉴权处理
     * @param serverWebExchange
     * @return
     */
    @Override
    public ServerHttpRequest authorize(ServerWebExchange serverWebExchange) {
        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
        HttpHeaders requestHeaders = serverHttpRequest.getHeaders();

        PairEntity<String> authorization = parseAuthorizationHeader(requestHeaders);
        String userName = authorization.getFirstEntity();
        String accessToken = authorization.getSecondEntity();
        String path = serverWebExchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String id = serverWebExchange.getAttribute(ROUTE_ID);

        Params params;

        // 目前只有访客,用户 2种,其余报错
        switch (userName) {
            case VISITOR:
                params = checkVisitorPermission(id, path, accessToken);
                break;
            case NEED_AUTHORIZATION:
                params = checkUserPermission(accessToken);
                break;
            default:
                throw new NoSuchUserNameMatchException();
        }

        // 对于校验通过的请求添加参数
        ServerHttpRequest newRequest = httpAuthorizationService.overwriteParams(serverHttpRequest, params);

        return newRequest;
    }

    /**
     * 检查访客权限
     * @param accessToken
     * @return
     */
    private Params checkVisitorPermission(String id, String path, String accessToken) {
        AuthenticationEntity authenticationEntity = httpAuthorizationService.verifyVisitorAccessToken(accessToken);
        Boolean verifyResult = authenticationEntity.getVerifyResult();
        if (!verifyResult) {
            throw new VisitorAccessTokenException();
        }

        Boolean existWhiteList = httpAuthorizationService.verifyVisitorExistWhiteList(id, path);
        if (!existWhiteList) {
            throw new VisitorNotInWhiteListException();
        }

        return new Params(UserVo.anonymousUser());
    }

    /**
     * 检查用户权限
     * @param accessToken
     * @return
     */
    private Params checkUserPermission(String accessToken) {
        logger.debug("访问令牌值 : {}", accessToken);
        AuthenticationEntity authenticationEntity = httpAuthorizationService.verifyUser(accessToken);
        Boolean verifyResult = authenticationEntity.getVerifyResult();
        RpcResponse<UserVo> rpcResponse = authenticationEntity.getRpcResponse();
        if (!verifyResult) {
            throw new AuthorizationFailedException(rpcResponse.getCode(), rpcResponse.getMessage());
        }

        UserVo userVo = authenticationEntity.getRpcResponse().getData();
        return new Params(userVo);
    }

    /**
     * 修改请求头信息
     * @param serverWebExchange
     * @return
     */
    @Override
    public ServerHttpRequest modifyRequestHeaders(ServerWebExchange serverWebExchange) {
        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
        HttpHeaders oldHeaders = serverHttpRequest.getHeaders();
        HttpHeaders httpHeaders = new HttpHeaders();

        logger.debug("处理请求头之前的头部信息 : {}", oldHeaders);
        copyMultiValueMap(oldHeaders, httpHeaders);
        logger.debug("处理请求头之后的头部信息 : {}", httpHeaders);

        httpModifyHeaderService.initRequestHeader(httpHeaders);
        httpModifyHeaderService.setVersion(httpHeaders);
        httpModifyHeaderService.setXEnv(httpHeaders);
        httpModifyHeaderService.removeInvalidInfo(httpHeaders);

        serverHttpRequest = new ServerHttpRequestDecorator(serverHttpRequest) {
            @Override
            public HttpHeaders getHeaders() {
                return httpHeaders;
            }
        };

        return serverHttpRequest;
    }

    /**
     * 修改返回体
     * @param serverWebExchange
     * @return
     */
    @Override
    public ServerHttpResponseDecorator modifyResponseBody(ServerWebExchange serverWebExchange) {
        ServerHttpResponse serverHttpResponse = serverWebExchange.getResponse();
        return httpModifyResponseService.modifyBody(serverHttpResponse);
    }

    /**
     * 修改返回体报头
     * @param serverWebExchange
     */
    @Override
    public void modifyResponseHeaders(ServerWebExchange serverWebExchange) {
        HttpHeaders httpHeaders = serverWebExchange.getResponse().getHeaders();
        httpModifyResponseService.modifyHeaders(httpHeaders);
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
        String[] arr;
        try {
            String authorizationHeader = headersOfAuthorization.get(0).substring(6);
            String decodeAuthorization = new String(Base64.decodeBase64(authorizationHeader), StandardCharsets.UTF_8);
            arr = decodeAuthorization.split(":");
        } catch (Exception e) {
            throw new ParseAuthorizationHeaderException();
        }

        return new PairEntity(arr[0], arr[1]);
    }

}
