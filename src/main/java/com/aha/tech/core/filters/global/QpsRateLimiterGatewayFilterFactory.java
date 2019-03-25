package com.aha.tech.core.filters.global;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.limiter.QpsRateLimiter;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.support.WriteResponseSupport;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.aha.tech.core.constant.GatewayFilterProcessOrderedConstant.GLOBAL_QPS_RATE_LIMITER_FILTER_ORDER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/3/20
 *
 * qps限流策略
 */
@Component
public class QpsRateLimiterGatewayFilterFactory implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(QpsRateLimiterGatewayFilterFactory.class);

    public static final String QPS_RATE_LIMITER_ERROR_MSG = "qps限流策略生效";

    private static final Long TIMEOUT = 2000L;

    @Resource
    private KeyResolver qpsResolver;

    @Resource
    private QpsRateLimiter qpsRateLimiter;


    @Value("${qps.ratelimiter.enable:false}")
    private boolean isEnable;

    @Override
    public int getOrder() {
        return GLOBAL_QPS_RATE_LIMITER_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("开始执行qps限流过滤器");

        if (!isEnable) {
            return chain.filter(exchange);
        }

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String key = qpsResolver.resolve(exchange).block();
        Mono<RateLimiter.Response> rateLimiterAllowed = qpsRateLimiter.isAllowed(route.getId(), key);
        try {
            if (rateLimiterAllowed.toFuture().get(TIMEOUT, TimeUnit.MILLISECONDS).isAllowed()) {
                return chain.filter(exchange);
            }
        } catch (InterruptedException e) {
            logger.error("执行qps限流时线程中断", e);
        } catch (ExecutionException e) {
            logger.error("执行qps限流时出现异常", e);
        } catch (TimeoutException e) {
            logger.error("获取令牌桶超时", e);
        }

        logger.error("没有通过qps限流");

        ResponseVo responseVo = ResponseVo.defaultFailureResponseVo();
        responseVo.setMessage(QPS_RATE_LIMITER_ERROR_MSG);
        return Mono.defer(() -> WriteResponseSupport.write(exchange,responseVo,HttpStatus.TOO_MANY_REQUESTS));
    }

}
