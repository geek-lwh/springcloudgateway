package com.aha.tech.core.service.impl;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.constant.SystemConstant;
import com.aha.tech.core.controller.FallBackController;
import com.aha.tech.core.model.entity.AuthenticationResultEntity;
import com.aha.tech.core.model.entity.PairEntity;
import com.aha.tech.core.service.*;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.passportserver.facade.code.AuthorizationCode;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import javax.annotation.Resource;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.*;
import static com.aha.tech.core.constant.HeaderFieldConstant.*;
import static com.aha.tech.core.constant.SystemConstant.TEST;
import static com.aha.tech.core.util.BeanUtil.copyMultiValueMap;

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

    @Value("${gateway.secret.key:4470c4bd3d88be85f031cce6bd907329}")
    private String secretKey;

    @Resource
    private VerifyRequestService httpVerifyRequestService;

    @Resource
    private RewritePathService httpRewritePathService;

    @Resource
    private AuthorizationService httpAuthorizationService;

    @Resource
    private ModifyHeaderService httpModifyHeaderService;

    @Resource
    private ModifyResponseService httpModifyResponseService;

    @Resource
    private WhiteListService whiteListService;

    @Override
    public Boolean isSkipIpLimiter(String rawPath) {
        List<String> list = whiteListService.findSkipIpLimiterWhiteList();
        if (CollectionUtils.isEmpty(list)) {
            return Boolean.FALSE;
        }
        return list.contains(rawPath);
    }

    @Override
    public Boolean isSkipAuth(String rawPath) {
        List<String> list = whiteListService.findSkipAuthWhiteList();
        if (CollectionUtils.isEmpty(list)) {
            return Boolean.FALSE;
        }
        return list.contains(rawPath);
    }

    @Override
    public Boolean isSkipUrlTamperProof(String rawPath, HttpHeaders httpHeaders) {
        String profile = System.getProperty(SystemConstant.SPRING_PROFILES_ACTIVE);
        if (profile.startsWith(TEST)) {
            List<String> isSkip = httpHeaders.getOrDefault(HEADER_SKIP_URL_TAMPER_PROOF, Collections.EMPTY_LIST);
            if (!CollectionUtils.isEmpty(isSkip)) {
                logger.info("存在跳过url防篡改的header头 : {}, profiles : {}", HEADER_SKIP_URL_TAMPER_PROOF, profile);
                return Boolean.TRUE;
            }
        }

        List<String> list = whiteListService.findSkipUrlTamperProofWhiteList();
        if (CollectionUtils.isEmpty(list)) {
            return Boolean.FALSE;
        }

        return list.contains(rawPath);
    }

    /**
     * url防篡改
     * version (Froyo, Gingerbread,IceCreamSandwich,JellyBean,KitKat,Lollipop)
     * @param version
     * @param timestamp
     * @param signature
     * @param rawPath
     * @param sortQueryParams
     * @return
     */
    @Override
    public Boolean urlTamperProof(String version, String timestamp, String signature, String rawPath, String sortQueryParams) {
        if (StringUtils.isBlank(timestamp)) {
            logger.error("URI防篡改timestamp缺失");
            return Boolean.FALSE;
        }

        if (StringUtils.isBlank(signature)) {
            logger.error("URI防篡改signature缺失");
            return Boolean.FALSE;
        }

        String encryptStr = Strings.EMPTY;

        switch (version) {
            case VERSION_FROYO:
                encryptStr = httpVerifyRequestService.verifyUrl(rawPath, sortQueryParams, timestamp, signature);
                break;
            default:
                logger.error("URI防篡改版本错误 version={}", version);
                break;
        }

        if (encryptStr.equals(signature)) {
            return Boolean.TRUE;
        }

        logger.error("url防篡改校验失败 rawPath:{} , sortQueryParams : {},timestamp : {},signature : {}, encrypt : {}, secretKey : {}", rawPath, sortQueryParams, timestamp, signature, encryptStr, secretKey);

        return Boolean.FALSE;
    }

    /**
     * body防篡改
     * @param version
     * @param body
     * @return
     */
    @Override
    public Boolean bodyTamperProof(String version, String body, String timestamp, String content) {
        String encryptStr = Strings.EMPTY;

        if ("{}".equals(body)) {
            logger.info("body 是{},赋值为空字符串");
            body = Strings.EMPTY;
        }

        if (StringUtils.isBlank(timestamp)) {
            logger.error("body防篡改timestamp缺失");
            return Boolean.FALSE;
        }

        if (StringUtils.isBlank(content)) {
            logger.error("body防篡改content缺失");
            return Boolean.FALSE;
        }

        switch (version) {
            case VERSION_FROYO:
                encryptStr = httpVerifyRequestService.verifyBody(body, timestamp, content);
                break;
            default:
                logger.error("URI防篡改版本错误 version={}", version);
                break;
        }

        return encryptStr.equals(content);
    }

    /**
     * 重写请求路径
     * @param serverWebExchange
     * @return
     */
    @Override
    public ServerHttpRequest rewriteRequestPath(ServerWebExchange serverWebExchange) {
        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();

        // 原始uri
        URI uri = serverHttpRequest.getURI();
        // 原始raw path
        String originalUrlPath = uri.getRawPath();
        // 去无效路径
        String validPath = httpRewritePathService.excludeInvalidPath(originalUrlPath, SKIP_STRIP_PREFIX_PART);
        // 获取routeId
        String routeId = StringUtils.substringBefore(validPath, Separator.SLASH_MARK);
        // 重写path
        String rewritePath = httpRewritePathService.rewritePath(routeId, validPath);

//        logger.info("uri : {},originalUrlPath : {},validPath : {},routeId : {},rewritePath : {}", uri, originalUrlPath, validPath, routeId, rewritePath);

        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR, originalUrlPath);
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_ROUTE_HOST_ATTR, uri);
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_VALID_PATH_ATTR, validPath);
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_REWRITE_PATH_ATTR, rewritePath);
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_ROUTE_ID_ATTR, routeId);
        // 生成新的request对象
        ServerHttpRequest newRequest = serverHttpRequest.mutate()
                .path(rewritePath)
                .build();

        logger.debug("结束重写请求路径,原路由路径 : {} , 新路由路径", rewritePath);

        return newRequest;
    }

    /**
     * 鉴权处理
     * @param serverWebExchange
     * @return
     */
    @Override
    public AuthenticationResultEntity authorize(ServerWebExchange serverWebExchange) {
        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
        Boolean isSkipAuth = ExchangeSupport.getIsSkipAuth(serverWebExchange);

        if (isSkipAuth) {
            logger.info("跳过授权认证 : {}", serverHttpRequest.getURI());
            return new AuthenticationResultEntity(isSkipAuth);
        }

        HttpHeaders requestHeaders = serverHttpRequest.getHeaders();
        // 解析authorization
        PairEntity<String> authorization = parseAuthorizationHeader(requestHeaders);
        if (authorization == null) {
            AuthenticationResultEntity failure = new AuthenticationResultEntity();
            failure.setWhiteList(Boolean.FALSE);
            failure.setCode(HttpStatus.UNAUTHORIZED.value());
            failure.setMessage(FallBackController.DEFAULT_SYSTEM_ERROR);
            return failure;
        }
        String userName = authorization.getFirstEntity();
        String accessToken = authorization.getSecondEntity();

        AuthenticationResultEntity authenticationResultEntity = checkPermission(serverWebExchange, userName, accessToken);
        authenticationResultEntity.setWhiteList(isSkipAuth);

        return authenticationResultEntity;
    }

    /**
     * 访问权限校验
     * @param swe
     * @param userName
     * @param accessToken
     * @return
     */
    private AuthenticationResultEntity checkPermission(ServerWebExchange swe, String userName, String accessToken) {
        if (VISITOR.equals(userName)) {
            return httpAuthorizationService.verifyVisitorAccessToken(swe, accessToken);
        }

        if (NEED_AUTHORIZATION.equals(userName)) {
            return httpAuthorizationService.verifyUser(swe, accessToken);
        }

        AuthenticationResultEntity authenticationResultEntity = new AuthenticationResultEntity();
        authenticationResultEntity.setCode(AuthorizationCode.SESSION_EXPIRED);
        logger.error("无效的user_name,userName : {},accessToken : {}", userName, accessToken);
        authenticationResultEntity.setMessage(FallBackController.DEFAULT_SYSTEM_ERROR);

        return authenticationResultEntity;
    }

    /**
     * 修改请求头信息
     * @param exchange
     * @param oldHeaders
     * @param remoteIp
     * @return
     */
    @Override
    public HttpHeaders modifyRequestHeaders(ServerWebExchange exchange, HttpHeaders oldHeaders, String remoteIp) {
        HttpHeaders newHeaders = new HttpHeaders();
        copyMultiValueMap(oldHeaders, newHeaders);

        httpModifyHeaderService.initHeaders(exchange, newHeaders, remoteIp);
        httpModifyHeaderService.versionSetting(newHeaders, exchange);
        httpModifyHeaderService.xEnvSetting(exchange, newHeaders);
        httpModifyHeaderService.removeHeaders(newHeaders);

        return newHeaders;
    }

    /**
     * 重新创建一个response对象
     * @param serverWebExchange
     * @return
     */
    @Override
    public ServerHttpResponseDecorator modifyResponseBodyAndHeaders(ServerWebExchange serverWebExchange) {
        ServerHttpResponse serverHttpResponse = serverWebExchange.getResponse();
        ServerHttpResponseDecorator serverHttpResponseDecorator = httpModifyResponseService.renewResponse(serverWebExchange, serverHttpResponse);
        return serverHttpResponseDecorator;
    }

    /**
     * 修改response body
     * @param serverWebExchange
     * @param serverHttpResponse
     * @return
     */
    @Override
    public ServerHttpResponseDecorator modifyResponseBody(ServerWebExchange serverWebExchange, ServerHttpResponse serverHttpResponse) {
        return httpModifyResponseService.renewResponse(serverWebExchange, serverHttpResponse);
    }

    /**
     //     * 修改response header
     //     * @param httpHeaders
     //     */
//    @Override
//    public void modifyResponseHeader(HttpHeaders httpHeaders) {
//        httpModifyResponseService.modifyResponseHeader(httpHeaders);
//    }

    /**
     * 解析http header 中的Authorization
     *
     * @param requestHeaders
     * @return
     */
    private PairEntity parseAuthorizationHeader(HttpHeaders requestHeaders) {
        List<String> headersOfAuthorization = requestHeaders.get(HEADER_AUTHORIZATION);
        if (CollectionUtils.isEmpty(headersOfAuthorization)) {
            logger.error("缺少Authorization 头对象 requestHeaders : {}", requestHeaders.toSingleValueMap().toString());
            return null;
        }

        String[] arr;
        try {
            String authorizationHeader = headersOfAuthorization.get(0).substring(6);
            String decodeAuthorization = new String(Base64.decodeBase64(authorizationHeader), StandardCharsets.UTF_8);
            arr = decodeAuthorization.split(":");
            return new PairEntity(arr[0], arr[1]);
        } catch (Exception e) {
            logger.error("Authorization 格式错误 ,Authorization : {}, requestHeaders : {}", headersOfAuthorization, requestHeaders.toSingleValueMap().toString());
            return null;
        }
    }

}
