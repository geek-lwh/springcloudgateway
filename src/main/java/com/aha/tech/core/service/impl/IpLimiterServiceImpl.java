package com.aha.tech.core.service.impl;

import com.aha.tech.core.limiter.IpRateLimiter;
import com.aha.tech.core.service.LimiterService;
import com.aha.tech.core.service.RequestHandlerService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.aha.tech.core.constant.AttributeConstant.GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR;
import static com.aha.tech.core.support.ParseHeadersSupport.parseHeaderIp;

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

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    /**
     * ip限流是否通过
     * @param exchange
     * @return
     */
    @Override
    public Boolean isAllowed(ServerWebExchange exchange) {
        exchange.getAttributes().put(GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR, exchange.getRequest().getURI().getRawPath());

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
        String remoteIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        String key = getKey(httpHeaders, remoteIp);

        Mono<RateLimiter.Response> rateLimiterAllowed = ipRateLimiter.isAllowed(route.getId(), key);

        Boolean isAllowed = Boolean.TRUE;
        try {
            RateLimiter.Response r = rateLimiterAllowed.toFuture().get(TIMEOUT, TimeUnit.MILLISECONDS);
//            ExchangeSupport.setIpLimiterCache(exchange, r, key);
            isAllowed = r.isAllowed();
        } catch (InterruptedException e) {
            logger.error("执行ip限流时线程中断", e);
        } catch (ExecutionException e) {
            logger.error("执行ip限流时出现异常", e);
        } catch (TimeoutException e) {
            logger.error("获取令牌桶超时", e);
        }

        return isAllowed;
    }

//    @Override
//    public Boolean isSkipLimiter(String rawPath) {
//        return httpRequestHandlerService.isSkipIpLimiter(rawPath);
//    }

    /**
     * 获取ip限流的key
     *
     * ip从头对象取出,获取最左一个
     * @param httpHeaders
     * @param remoteIp
     * @return
     */
    private String getKey(HttpHeaders httpHeaders, String remoteIp) {
        String realIp = parseHeaderIp(httpHeaders);
        if (StringUtils.isBlank(realIp)) {
            logger.warn("缺失X-Forwarded-For , 默认值 : {}", remoteIp);
            realIp = remoteIp;
        }

        return realIp;
    }

}
