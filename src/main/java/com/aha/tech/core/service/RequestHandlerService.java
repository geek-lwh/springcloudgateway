package com.aha.tech.core.service;

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

    /**
     * 打印访问日志
     * @param serverWebExchange
     */
//    void writeAccessInfo(ServerWebExchange serverWebExchange);

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
    Boolean authorize(ServerWebExchange serverWebExchange);

    /**
     * 修改请求头
     * @param httpHeaders
     * @return
     */
    HttpHeaders modifyRequestHeaders(HttpHeaders httpHeaders, String remoteIp);

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
    void modifyResponseHeader(HttpHeaders httpHeaders);
}
