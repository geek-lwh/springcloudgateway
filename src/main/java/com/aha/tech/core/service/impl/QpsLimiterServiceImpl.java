package com.aha.tech.core.service.impl;

import com.aha.tech.core.limiter.QpsRateLimiter;
import com.aha.tech.core.service.LimiterService;
import com.aha.tech.util.KeyGenerateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @Author: luweihong
 * @Date: 2019/3/25
 */
@Service("qpsLimiterService")
public class QpsLimiterServiceImpl implements LimiterService {

    private static final Logger logger = LoggerFactory.getLogger(QpsLimiterServiceImpl.class);

    private static final Long TIMEOUT = 1000L;

    @Resource
    private QpsRateLimiter qpsRateLimiter;

    /**
     * qps限流是否通过
     * @param exchange
     * @return
     */
    @Override
    public Boolean isAllowed(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String key = getKey();
        Mono<RateLimiter.Response> rateLimiterAllowed = qpsRateLimiter.isAllowed(route.getId(), key);

        Boolean isAllowed = Boolean.TRUE;
        try {
            isAllowed =rateLimiterAllowed.toFuture().get(TIMEOUT, TimeUnit.MILLISECONDS).isAllowed();
        } catch (InterruptedException e) {
            logger.error("执行qps限流时线程中断", e);
        } catch (ExecutionException e) {
            logger.error("执行qps限流时出现异常", e);
        } catch (TimeoutException e) {
            logger.error("获取令牌桶超时", e);
        }

        return isAllowed;
    }

    /**
     * 获取限流器的key
     * @return
     */
    private String getKey() {
        return KeyGenerateUtil.GatewayLimiter.qpsLimiterKey();
    }

}
