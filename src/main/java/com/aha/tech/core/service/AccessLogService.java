package com.aha.tech.core.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

/**
 * @Author: luweihong
 * @Date: 2019/4/9
 */
public interface AccessLogService {

    /**
     * 打印请求信息
     * @param exchange
     * @param cost
     * @return
     */
    String requestLog(ServerWebExchange exchange, Long cost,String responseVo);

    String printAccessLogging(ServerHttpRequest serverHttpRequest, Long startTime, Long endTime, HttpStatus status);
    /**
     * 当遇到错误时,打印详细信息
     *
     * httpHeaders
     */
    void printWhenError(ServerWebExchange serverWebExchange, Exception e);

}
