package com.aha.tech.core.service;

import org.springframework.web.server.ServerWebExchange;

/**
 * @Author: luweihong
 * @Date: 2019/3/25
 */
public interface LimiterService {

    /**
     * 限流算法是否通过
     * @param exchange
     * @return
     */
    Boolean isAllowed(ServerWebExchange exchange);

    /**
     * 是否跳过限流
     * @param rawPath
     * @return
     */
//    Boolean isSkipLimiter(String rawPath);
}
