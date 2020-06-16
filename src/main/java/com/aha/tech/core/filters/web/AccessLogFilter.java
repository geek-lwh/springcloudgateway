package com.aha.tech.core.filters.web;

import com.aha.tech.core.service.AccessLogService;
import com.aha.tech.core.support.ExchangeSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * @Author: luweihong
 * @Date: 2019/4/10
 *
 * 访问日志打印过滤器
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AccessLogFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(AccessLogFilter.class);

    @Resource
    private ThreadPoolTaskExecutor writeLoggingThreadPool;

    @Resource
    private AccessLogService httpAccessLogService;

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
        Long startTime = System.currentTimeMillis();
        String traceId = ExchangeSupport.getTraceId(serverWebExchange);
        return webFilterChain.filter(serverWebExchange)
                .doFinally((s) -> CompletableFuture.runAsync(() -> {
                    MDC.put("traceId", traceId);
                    Long cost = System.currentTimeMillis() - startTime;
                    logger.info("Request Info : {}", httpAccessLogService.requestLog(serverWebExchange, cost, ExchangeSupport.getResponseBody(serverWebExchange)));
                }, writeLoggingThreadPool));
    }


}
