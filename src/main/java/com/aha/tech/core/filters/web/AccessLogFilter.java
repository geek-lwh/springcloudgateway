package com.aha.tech.core.filters.web;

import com.aha.tech.core.service.AccessLogService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.util.TracerUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
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

import static com.aha.tech.core.constant.ExchangeAttributeConstant.TRACE_LOG_ID;

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
        Tracer tracer = GlobalTracer.get();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(serverWebExchange.getRequest().getURI().toString());
        Span span = spanBuilder.start();
        try (Scope scope = tracer.scopeManager().activate(span)) {
            ExchangeSupport.setSpan(serverWebExchange, span);
            TracerUtils.setClue(span);
            Long startTime = System.currentTimeMillis();
            return webFilterChain.filter(serverWebExchange)
                    .doFinally((s) -> CompletableFuture.runAsync(() -> {
                        String traceId = serverWebExchange.getAttributeOrDefault(TRACE_LOG_ID, "MISS_TRACE_ID");
                        MDC.put("traceId", traceId);
                        Long cost = System.currentTimeMillis() - startTime;
                        logger.info("response Info : {}", httpAccessLogService.requestLog(serverWebExchange, cost, ExchangeSupport.getResponseBody(serverWebExchange)));
                    }, writeLoggingThreadPool));
        } catch (Exception e) {
            TracerUtils.reportErrorTrace(e);
            throw e;
        } finally {
            span.finish();
        }


    }


}
