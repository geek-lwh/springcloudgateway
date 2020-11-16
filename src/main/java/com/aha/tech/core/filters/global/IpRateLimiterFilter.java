package com.aha.tech.core.filters.global;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.controller.FallBackController;
import com.aha.tech.core.exception.LimiterException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.LimiterService;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.aha.tech.util.TracerUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.TRACE_LOG_ID;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.IP_RATE_LIMITER_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/3/20
 *
 * qps限流策略
 */
@Component
public class IpRateLimiterFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(IpRateLimiterFilter.class);

    public static final String IP_RATE_LIMITER_ERROR_MSG = "IP限流策略生效";

    @Resource
    private LimiterService ipLimiterService;

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Value("${ip.ratelimiter.enable:false}")
    private boolean isEnable;

    @Override
    public int getOrder() {
        return IP_RATE_LIMITER_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Tracer tracer = GlobalTracer.get();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(this.getClass().getName());
        Span parentSpan = ExchangeSupport.getSpan(exchange);
        Span span = spanBuilder.asChildOf(parentSpan).start();
        ExchangeSupport.setSpan(exchange, span);
        try (Scope scope = tracer.scopeManager().activate(span)) {
            TracerUtils.setClue(span);
            ExchangeSupport.put(exchange, TRACE_LOG_ID, span.context().toTraceId());
            return getResult(exchange, chain);
        } catch (Exception e) {
            TracerUtils.reportErrorTrace(e);
            throw e;
        } finally {
            span.finish();
        }
    }

    /**
     * 获取ip限流结果
     * @param exchange
     * @param chain
     * @return
     */
    private Mono<Void> getResult(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isEnable) {
            return chain.filter(exchange);
        }

        String traceId = exchange.getAttributeOrDefault(TRACE_LOG_ID, "MISS_TRACE_ID");
        MDC.put("traceId", traceId);

        String rawPath = exchange.getRequest().getURI().getRawPath();
        if (httpRequestHandlerService.isSkipIpLimiter(rawPath)) {
            logger.info("跳过ip限流策略 : {}", rawPath);
            return chain.filter(exchange);
        }

        Boolean isAllowed = ipLimiterService.isAllowed(exchange);
        if (isAllowed) {
            return chain.filter(exchange);
        }

        logger.error("ip : {} 限流算法生效", exchange.getRequest().getHeaders().get(HeaderFieldConstant.HEADER_X_FORWARDED_FOR));
        final ResponseVo responseVo = new ResponseVo(HttpStatus.TOO_MANY_REQUESTS.value(), FallBackController.DEFAULT_SYSTEM_ERROR);
        return Mono.defer(() -> ResponseSupport.write(exchange, responseVo, HttpStatus.TOO_MANY_REQUESTS, new LimiterException(IP_RATE_LIMITER_ERROR_MSG)));
    }

}
