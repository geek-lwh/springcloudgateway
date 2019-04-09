package com.aha.tech.core.service;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.Map;

/**
 * @Author: luweihong
 * @Date: 2019/4/9
 */
public interface AccessLogService {

    /**
     * 打印http请求信息
     * @param serverHttpRequest
     * @param attributes
     */
    void printRequestInfo(ServerHttpRequest serverHttpRequest, String id, Long requestTime);

    void printResponseInfo(ServerHttpResponse serverHttpResponse, Map<String, Object> attributes);

}
