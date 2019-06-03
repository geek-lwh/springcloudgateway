package com.aha.tech.core.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

/**
 * @Author: luweihong
 * @Date: 2019/4/9
 */
public interface AccessLogService {

    String printAccessLogging(ServerHttpRequest serverHttpRequest, Long startTime, Long endTime, HttpStatus status);
    /**
     * 当遇到错误时,打印详细信息
     *
     * httpHeaders
     */
    void printWhenError(ServerWebExchange serverWebExchange, Exception e);

}
