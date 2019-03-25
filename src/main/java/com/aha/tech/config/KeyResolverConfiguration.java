package com.aha.tech.config;

import com.aha.tech.core.exception.MissHeaderXForwardedException;
import com.aha.tech.core.exception.XForwardedEmptyException;
import com.aha.tech.core.limiter.IpRateLimiter;
import com.aha.tech.core.limiter.QpsRateLimiter;
import com.aha.tech.util.KeyGenerateUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Validator;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_ROUTE_ID_ATTR;
import static com.aha.tech.core.constant.HeaderFieldConstant.HEADER_X_FORWARDED_FOR;

/**
 * @Author: luweihong
 * @Date: 2019/3/19
 */
@Component
public class KeyResolverConfiguration {


    /**
     * 全局限流
     * @return
     */
    @Bean("qpsResolver")
    public KeyResolver qpsResolver() {
        return exchange -> Mono.just(KeyGenerateUtil.GatewayLimiter.qpsLimiterKey());
    }

    /**
     * ip总限流
     *
     * @return
     */
    @Bean("ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            HttpHeaders httpHeaders = exchange.getResponse().getHeaders();
            List<String> forwardedList = httpHeaders.get(HEADER_X_FORWARDED_FOR);
            if(CollectionUtils.isEmpty(forwardedList)){
                throw new MissHeaderXForwardedException();
            }

            String keyResolver = forwardedList.get(0);
            if(StringUtils.isBlank(keyResolver)){
                throw new XForwardedEmptyException();
            }

            return Mono.just(forwardedList.get(0));
        };
    }

    /**
     * api限流
     *
     * 请求的原始path作为key值
     * @return
     */
    @Bean("apiKeyResolver")
    KeyResolver apiKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getURI().getRawPath());
    }

    /**
     * 服务限流
     *
     * 基于服务id做限流
     * 默认不启用
     * @return
     */
    @Bean("serverKeyResolver")
    KeyResolver serverKeyResolver() {
        return exchange -> Mono.just(exchange.getAttributes().get(GATEWAY_REQUEST_ROUTE_ID_ATTR).toString());
    }

    /**
     * qps限流初始化
     * @param redisTemplate
     * @param script
     * @param validator
     * @return
     */
    @Primary
    @Bean("qpsRateLimiter")
    QpsRateLimiter qpsRateLimiter(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Qualifier(QpsRateLimiter.REDIS_SCRIPT_NAME) RedisScript<List<Long>> script,
            Validator validator) {
        return new QpsRateLimiter(redisTemplate, script, validator);
    }

    /**
     * ip限流初始化
     * @param redisTemplate
     * @param script
     * @param validator
     * @return
     */
    @Bean("ipRateLimiter")
    IpRateLimiter ipRateLimiter(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Qualifier(QpsRateLimiter.REDIS_SCRIPT_NAME) RedisScript<List<Long>> script,
            Validator validator) {
        return new IpRateLimiter(redisTemplate, script, validator);
    }

}
