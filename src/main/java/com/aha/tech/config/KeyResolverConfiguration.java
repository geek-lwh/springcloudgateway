package com.aha.tech.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_ROUTE_ID_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/3/19
 */
@Component
public class KeyResolverConfiguration {

    /**
     * ip总限流
     *
     * @return
     */
    @Bean("ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
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
     * 使用自己定义的限流类
     * @param redisTemplate
     * @param script
     * @param validator
     * @return
     */
    @Primary
    @Bean("ipRateLimiter")
    IpRateLimiter ipRateLimiter(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Qualifier(IpRateLimiter.REDIS_SCRIPT_NAME) RedisScript<List<Long>> script,
            Validator validator) {
        return new IpRateLimiter(redisTemplate, script, validator);
    }

}
