package com.aha.tech.core.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
public interface ModifyResponseService {

    /**
     * 修改返回体
     * @param serverHttpResponse
     * @return
     */
    ServerHttpResponseDecorator modifyBody(ServerHttpResponse serverHttpResponse);

    /**
     * 修改返回对象的报头
     * @param httpHeaders
     */
    void modifyHeaders(HttpHeaders httpHeaders);

}
