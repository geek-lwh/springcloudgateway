package com.aha.tech.core.service.impl;

import com.aha.tech.core.exception.MissHeaderXForwardedException;
import com.aha.tech.core.exception.XForwardedEmptyException;
import com.aha.tech.core.limiter.IpRateLimiter;
import com.aha.tech.core.service.LimiterService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.aha.tech.core.constant.HeaderFieldConstant.HEADER_X_FORWARDED_FOR;

/**
 * @Author: luweihong
 * @Date: 2019/3/25
 */
@Service("ipLimiterService")
public class IpLimiterServiceImpl implements LimiterService {

    private static final Logger logger = LoggerFactory.getLogger(IpLimiterServiceImpl.class);

    private static final Long TIMEOUT = 1000L;

    @Resource
    private IpRateLimiter ipRateLimiter;

    /**
     * ip限流是否通过
     * @param exchange
     * @return
     */
    @Override
    public Boolean isAllowed(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
        String key = getKey(httpHeaders);

        Mono<RateLimiter.Response> rateLimiterAllowed = ipRateLimiter.isAllowed(route.getId(), key);

        Boolean isAllowed = Boolean.TRUE;
        try {
            isAllowed = rateLimiterAllowed.toFuture().get(TIMEOUT, TimeUnit.MILLISECONDS).isAllowed();
        } catch (InterruptedException e) {
            logger.error("执行ip限流时线程中断", e);
        } catch (ExecutionException e) {
            logger.error("执行ip限流时出现异常", e);
        } catch (TimeoutException e) {
            logger.error("获取令牌桶超时", e);
        }

        return isAllowed;
    }

    /**
     * 获取ip限流的key
     * @param httpHeaders
     * @return
     */
    private String getKey(HttpHeaders httpHeaders) {
        List<String> forwardedList = httpHeaders.get(HEADER_X_FORWARDED_FOR);
        if (CollectionUtils.isEmpty(forwardedList)) {
            throw new MissHeaderXForwardedException();
        }

        String keyResolver = forwardedList.get(0);
        if (StringUtils.isBlank(keyResolver)) {
            throw new XForwardedEmptyException();
        }

        return forwardedList.get(0);
    }

}
