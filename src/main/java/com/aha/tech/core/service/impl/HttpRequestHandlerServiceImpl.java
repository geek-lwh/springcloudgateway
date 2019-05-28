package com.aha.tech.core.service.impl;

import com.aha.tech.core.constant.LanguageConstant;
import com.aha.tech.core.exception.AuthorizationFailedException;
import com.aha.tech.core.exception.MissAuthorizationHeaderException;
import com.aha.tech.core.exception.NoSuchUserNameException;
import com.aha.tech.core.exception.ParseAuthorizationHeaderException;
import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.model.entity.AuthenticationEntity;
import com.aha.tech.core.model.entity.PairEntity;
import com.aha.tech.core.model.entity.RouteEntity;
import com.aha.tech.core.service.*;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
import static com.aha.tech.core.constant.HeaderFieldConstant.VERSION_FROYO;
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
    public Boolean isSkipUrlTamperProof(String rawPath) {
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
     * @param url
     * @return
     */
    @Override
    public Boolean urlTamperProof(String version, String timestamp, String signature, String rawPath, String url) {
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
                encryptStr = httpVerifyRequestService.verifyUrl(rawPath, url, timestamp);
                break;
            default:
                logger.error("URI防篡改版本错误 version={}", version);
                break;
        }

        if (encryptStr.equals(signature)) {
            return Boolean.TRUE;
        }

        logger.error("url防篡改校验失败 uri : {},timestamp : {},signature : {}", url, timestamp, signature);

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

        if (StringUtils.isBlank(body)) {
            logger.error("body防篡改body缺失");
            return Boolean.FALSE;
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
                encryptStr = httpVerifyRequestService.verifyBody(body, timestamp);
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

        URI uri = serverHttpRequest.getURI();
        String originalUrlPath = uri.getRawPath();
        logger.debug("开始重写请求路径,原路由路径 : {}", originalUrlPath);
        String validPath = httpRewritePathService.excludeInvalidPath(originalUrlPath, SKIP_STRIP_PREFIX_PART);

        // 重写请求路径
        RouteEntity routeEntity = httpRewritePathService.rewritePath(validPath);
        String rewritePath = routeEntity.getRewritePath();
        String id = routeEntity.getId();
        String language = StringUtils.isBlank(routeEntity.getLanguage()) ? LanguageConstant.JAVA : routeEntity.getLanguage().toLowerCase();

        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR, originalUrlPath);
        serverWebExchange.getAttributes().put(REQUEST_LANGUAGE_ATTR, language);
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_VALID_PATH_ATTR, validPath);
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_REWRITE_PATH_ATTR, rewritePath);
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_ROUTE_ID_ATTR, id);
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
    public Boolean authorize(ServerWebExchange serverWebExchange) {
        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
        Boolean isSkipAuth = ExchangeSupport.getIsSkipAuth(serverWebExchange);
        if (isSkipAuth) {
            logger.info("跳过授权认证 : {}", serverHttpRequest.getURI());
            return Boolean.TRUE;
        }

        HttpHeaders requestHeaders = serverHttpRequest.getHeaders();
        // 解析authorization
        PairEntity<String> authorization = parseAuthorizationHeader(requestHeaders);
        String userName = authorization.getFirstEntity();
        String accessToken = authorization.getSecondEntity();
        logger.debug("user name : {},access token : {},authorization : {}", userName, accessToken, authorization);
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
        AuthenticationEntity authenticationEntity;

        if (VISITOR.equals(userName)) {
            authenticationEntity = httpAuthorizationService.verifyVisitorAccessToken(accessToken);
        } else if (NEED_AUTHORIZATION.equals(userName)) {
            authenticationEntity = httpAuthorizationService.verifyUser(accessToken);
        } else {
            logger.error("无效的user_name : {}", userName);
            throw new NoSuchUserNameException(String.format("无效的user_name对象 : %s", userName));
        }

        RequestAddParamsDto requestAddParamsDto = new RequestAddParamsDto();
        UserVo userVo = authenticationEntity.getUserVo();
        if (userVo == null) {
            logger.error("用户授权信息异常,获取的用户对象为空,access token : {}", accessToken);
            throw new AuthorizationFailedException();
        }

        requestAddParamsDto.setUserId(userVo.getUserId());
        serverWebExchange.getAttributes().put(GATEWAY_REQUEST_ADD_PARAMS_ATTR, requestAddParamsDto);

        return authenticationEntity.getVerifyResult();
    }

    /**
     * 修改请求头信息
     * @param oldHeaders
     * @return
     */
    @Override
    public HttpHeaders modifyRequestHeaders(HttpHeaders oldHeaders, String remoteIp) {
        HttpHeaders newHeaders = new HttpHeaders();
        copyMultiValueMap(oldHeaders, newHeaders);

        httpModifyHeaderService.initHeaders(newHeaders, remoteIp);
        httpModifyHeaderService.versionSetting(newHeaders);
        httpModifyHeaderService.xEnvSetting(newHeaders);
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
     * 修改response header
     * @param httpHeaders
     */
    @Override
    public void modifyResponseHeader(HttpHeaders httpHeaders) {
        httpModifyResponseService.crossAccessSetting(httpHeaders);
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
