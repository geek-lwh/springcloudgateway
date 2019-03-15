package com.aha.tech.core.service;

import com.aha.tech.core.model.entity.AuthenticationEntity;
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
    ServerHttpRequest authorize(ServerWebExchange serverWebExchange);

    /**
     * 修改请求头
     * @param serverWebExchange
     * @return
     */
    ServerHttpRequest modifyRequestHeaders(ServerWebExchange serverWebExchange);

    /**
     * 修改返回体信息
     * @param serverWebExchange
     * @return
     */
    ServerHttpResponseDecorator modifyResponseBody(ServerWebExchange serverWebExchange);

    /**
     * 修改返回报头信息
     * @param serverWebExchange
     */
    void modifyResponseHeaders(ServerWebExchange serverWebExchange);

}
