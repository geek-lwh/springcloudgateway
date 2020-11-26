package com.aha.tech.core.service;

import org.springframework.web.server.ServerWebExchange;

/**
 * @Author: luweihong
 * @Date: 2019/4/9
 */
public interface AccessLogService {

    /**
     * 当遇到错误时,打印详细信息
     *
     * httpHeaders
     */
    void asyncLogError(ServerWebExchange serverWebExchange, Exception e);

}
