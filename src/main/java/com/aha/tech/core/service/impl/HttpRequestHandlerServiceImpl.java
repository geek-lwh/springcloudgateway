package com.aha.tech.core.service.impl;

import com.aha.tech.core.exception.MissAuthorizationHeaderException;
import com.aha.tech.core.exception.ParseAuthorizationHeaderException;
import com.aha.tech.core.model.entity.AuthenticationEntity;
import com.aha.tech.core.model.entity.PairEntity;
import com.aha.tech.core.model.entity.RouteEntity;
import com.aha.tech.core.service.*;
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

import static com.aha.tech.core.constant.ExchangeAttributeConstant.*;
import static com.aha.tech.core.constant.HeaderFieldConstant.HEADER_AUTHORIZATION;
import static com.aha.tech.core.tools.BeanUtil.copyMultiValueMap;

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
        String originalUrlPath = uri.getRawPath();
        logger.info("开始重写请求路径,原路由路径 : {}", originalUrlPath);

        // 去除路径中无效的字符
        String validPath = httpRewritePathService.excludeInvalidPath(originalUrlPath, SKIP_STRIP_PREFIX_PART);

        // 重写请求路径
        RouteEntity routeEntity = httpRewritePathService.rewritePath(validPath);
        String rewritePath = routeEntity.getRewritePath();
        String id = routeEntity.getId();

        serverWebExchange.getAttributes().put(GATEWAY_ORIGINAL_URL_PATH_ATTR, originalUrlPath);
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_VALID_PATH_ATTR, validPath);
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_REWRITE_PATH_ATTR, rewritePath);
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_ROUTE_ID_ATTR, id);
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
    public Boolean authorize(ServerWebExchange serverWebExchange) {
        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
        HttpHeaders requestHeaders = serverHttpRequest.getHeaders();

        PairEntity<String> authorization = parseAuthorizationHeader(requestHeaders);
        String userName = authorization.getFirstEntity();
        String accessToken = authorization.getSecondEntity();

        return checkPermission(serverWebExchange, userName, accessToken);
    }

    /**
     * 访问权限校验
     * @param serverWebExchange
     * @param userName
     * @param accessToken
     * @return
     */
    private Boolean checkPermission(ServerWebExchange serverWebExchange, String userName, String accessToken) {
        AuthenticationEntity authenticationEntity = new AuthenticationEntity();
        if (VISITOR.equals(userName)) {
            String path = serverWebExchange.getAttribute(GATEWAY_REQUEST_VALID_PATH_ATTR).toString();
            String id = serverWebExchange.getAttribute(GATEWAY_REQUEST_ROUTE_ID_ATTR).toString();
            authenticationEntity = httpAuthorizationService.verifyVisitorAccessToken(id, path, accessToken);
        }

        if (NEED_AUTHORIZATION.equals(userName)) {
            authenticationEntity = httpAuthorizationService.verifyUser(accessToken);
        }

        serverWebExchange.getAttributes().put(GATEWAY_USER_VO_ATTR, authenticationEntity.getUserVo());

        return Boolean.TRUE;
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
        return httpModifyResponseService.modifyBody(serverWebExchange, serverHttpResponse);
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
            return new PairEntity(arr[0], arr[1]);
        } catch (Exception e) {
            logger.error("解析请求头Authorization字段出现异常,Authorization : {}", headersOfAuthorization);
            throw new ParseAuthorizationHeaderException();
        }

    }

}
