package com.aha.tech.core.filters.global;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.controller.FallBackController;
import com.aha.tech.core.exception.LimiterException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.LimiterService;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.aha.tech.util.LogUtil;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;

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
        LogUtil.combineTraceId(exchange);
        Boolean isAllowed = isIpAllowed(exchange);

        if (!isAllowed) {
            List<String> ipList = exchange.getRequest().getHeaders().getOrDefault(HeaderFieldConstant.HEADER_X_FORWARDED_FOR, Lists.newArrayList("no ip"));
            logger.error("ip : {} 限流算法生效", ipList.get(0));
            ExchangeSupport.setHttpStatus(exchange, HttpStatus.TOO_MANY_REQUESTS);
            ExchangeSupport.fillErrorMsg(exchange, "IP限流算法生效");

            final ResponseVo responseVo = new ResponseVo(HttpStatus.TOO_MANY_REQUESTS.value(), FallBackController.DEFAULT_SYSTEM_ERROR);
            return Mono.defer(() -> ResponseSupport.interrupt(exchange, responseVo, HttpStatus.TOO_MANY_REQUESTS, new LimiterException(IP_RATE_LIMITER_ERROR_MSG)));
        }

        return chain.filter(exchange);
    }

    /**
     * 获取ip限流结果
     * @param exchange
     * @return
     */
    private Boolean isIpAllowed(ServerWebExchange exchange) {
        if (!isEnable) {
            return Boolean.TRUE;
        }

        String rawPath = exchange.getRequest().getURI().getRawPath();
        if (httpRequestHandlerService.isSkipIpLimiter(rawPath)) {
            logger.info("跳过ip限流策略 : {}", rawPath);
            return Boolean.TRUE;
        }

        Boolean isAllowed = ipLimiterService.isAllowed(exchange);

        return isAllowed;
    }

}
