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
    void writeAccessInfo(ServerWebExchange serverWebExchange);

    /**
     * 打印结果
     * @param serverWebExchange
     */
    void writeResultInfo(ServerWebExchange serverWebExchange);

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
    HttpHeaders modifyRequestHeaders(HttpHeaders httpHeaders);

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
