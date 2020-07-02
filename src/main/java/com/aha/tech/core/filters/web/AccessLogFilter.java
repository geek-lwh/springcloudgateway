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
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.aha.tech.core.constant.HeaderFieldConstant.REQUEST_ID;
import static com.aha.tech.core.constant.HeaderFieldConstant.X_TRACE_ID;
import static com.aha.tech.core.interceptor.FeignRequestInterceptor.TRACE_ID;

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
        return webFilterChain.filter(serverWebExchange)
                .doFinally((s) -> CompletableFuture.runAsync(() -> {
                    List<String> clientRequestId =serverWebExchange.getRequest().getHeaders().get(REQUEST_ID);
                    if (!CollectionUtils.isEmpty(clientRequestId)) {
                        MDC.put(TRACE_ID, clientRequestId.get(0));
                    }
                    Long cost = System.currentTimeMillis() - startTime;
                    logger.info("response Info : {}", httpAccessLogService.requestLog(serverWebExchange, cost, ExchangeSupport.getResponseBody(serverWebExchange)));
                }, writeLoggingThreadPool));
    }


}
