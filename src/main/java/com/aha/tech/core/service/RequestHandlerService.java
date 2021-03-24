package com.aha.tech.core.service;

import com.aha.tech.core.model.entity.AuthenticationResultEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
public interface RequestHandlerService {

    Boolean isSkipIpLimiter(String api, String ip);

    /**
     * 是否跳过授权验证
     * @param rawPath
     * @return
     */
    Boolean isSkipAuth(String rawPath);

    /**
     * 是否在孩子账户出现5300错误时跳过
     * @param rawPath
     * @return
     */
    Boolean isIgnoreEmptyKidMapping(String rawPath);

    /**
     * 是否跳过url防篡改
     * @param rawPath
     * @param httpHeaders
     * @return
     */
    Boolean isSkipUrlTamperProof(String rawPath, HttpHeaders httpHeaders);

    /**
     * url防篡改
     * @param version
     * @param timestamp
     * @param signature
     * @param url
     * @return
     */
    Boolean urlTamperProof(String version, String timestamp, String signature, String rawPath, String url);

    /**
     * body防篡改
     * @param version
     * @param body
     * @return
     */
    Boolean bodyTamperProof(String version, String body, String timestamp, String content);
    /**
     * 重写请求路径
     * @param serverWebExchange
     * @return
     */
    ServerHttpRequest rewriteRequestPath(ServerWebExchange serverWebExchange);

    /**
     * 鉴权处理
     * @param serverWebExchange
     * @return
     */
    AuthenticationResultEntity authorize(ServerWebExchange serverWebExchange);

    /**
     * 修改请求头
     * @param exchange
     * @param httpHeaders
     * @param remoteIp
     * @return
     */
    HttpHeaders modifyRequestHeaders(ServerWebExchange exchange, HttpHeaders httpHeaders, String remoteIp);

    /**
     * 重新创建一个response对象
     * @param serverWebExchange
     * @return
     */
    ServerHttpResponseDecorator modifyResponseBodyAndHeaders(ServerWebExchange serverWebExchange);

    /**
     * 修改body
     * @param serverWebExchange
     * @param serverHttpResponse
     * @return
     */
    ServerHttpResponseDecorator modifyResponseBody(ServerWebExchange serverWebExchange,ServerHttpResponse serverHttpResponse);

    /**
     * 修改response header
     * @param httpHeaders
     * @return
     */
//    void modifyResponseHeader(HttpHeaders httpHeaders);
}
