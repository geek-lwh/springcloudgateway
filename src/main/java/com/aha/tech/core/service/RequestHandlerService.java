package com.aha.tech.core.service;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
public interface RequestHandlerService {

    void crossDomainAccessSetting(ServerWebExchange serverWebExchange);

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
     * @param serverWebExchange
     * @return
     */
    ServerHttpRequest modifyRequestHeaders(ServerWebExchange serverWebExchange);

}
