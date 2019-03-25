package com.aha.tech.config;

import com.aha.tech.core.limiter.IpRateLimiter;
import com.aha.tech.core.limiter.QpsRateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;

import java.util.List;

/**
 * @Author: luweihong
 * @Date: 2019/3/19
 */
@Component
public class RateLimiterConfiguration {

    /**
     * qps限流初始化
     * @param redisTemplate
     * @param script
     * @param validator
     * @return
     */
    @Primary
    @Bean
    QpsRateLimiter initQpsRateLimiter(
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
    @Bean
    IpRateLimiter initIpRateLimiter(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Qualifier(QpsRateLimiter.REDIS_SCRIPT_NAME) RedisScript<List<Long>> script,
            Validator validator) {
        return new IpRateLimiter(redisTemplate, script, validator);
    }

}
