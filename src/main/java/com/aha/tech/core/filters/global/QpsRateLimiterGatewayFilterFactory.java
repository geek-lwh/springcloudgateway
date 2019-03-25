package com.aha.tech.core.filters.global;

import com.aha.tech.core.exception.GatewayException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.LimiterService;
import com.aha.tech.core.support.WriteResponseSupport;
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

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.GLOBAL_QPS_RATE_LIMITER_FILTER_ORDER;

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

    @Resource
    private LimiterService qpsLimiterService;


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

        try {
            Boolean isAllowed = qpsLimiterService.isAllowed(exchange);
            if (isAllowed) {
                return chain.filter(exchange);
            }
        } catch (GatewayException e) {
            logger.error("qps限流出现异常", e);
            final ResponseVo responseVo = new ResponseVo(e.getCode(), e.getMessage());
            return Mono.defer(() -> WriteResponseSupport.write(exchange, responseVo, HttpStatus.BAD_GATEWAY));
        }

        logger.error("没有通过qps限流");

        ResponseVo responseVo = ResponseVo.defaultFailureResponseVo();
        responseVo.setMessage(QPS_RATE_LIMITER_ERROR_MSG);
        return Mono.defer(() -> WriteResponseSupport.write(exchange,responseVo,HttpStatus.TOO_MANY_REQUESTS));
    }

}
