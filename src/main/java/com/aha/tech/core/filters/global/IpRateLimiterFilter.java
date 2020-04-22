package com.aha.tech.core.filters.global;

import com.aha.tech.core.exception.LimiterException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.LimiterService;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

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
        logger.debug("开始执行ip限流过滤器");

        if (!isEnable) {
            return chain.filter(exchange);
        }
        String traceId = ExchangeSupport.getTraceId(exchange);
        MDC.put("traceId",traceId);
        String rawPath = exchange.getRequest().getURI().getRawPath();
        if (httpRequestHandlerService.isSkipIpLimiter(rawPath)) {
            logger.info("跳过ip限流策略 : {}", rawPath);
            return chain.filter(exchange);
        }

        Boolean isAllowed = ipLimiterService.isAllowed(exchange);
        if (isAllowed) {
            return chain.filter(exchange);
        }

        logger.warn("ip : {} 限流算法生效", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        final ResponseVo responseVo = ResponseVo.defaultFailureResponseVo();
        responseVo.setMessage(IP_RATE_LIMITER_ERROR_MSG);
        return Mono.defer(() -> ResponseSupport.write(exchange, responseVo, new LimiterException(IP_RATE_LIMITER_ERROR_MSG)));
    }

}
