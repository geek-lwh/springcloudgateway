package com.aha.tech.core.limiter;

import com.aha.tech.core.support.LimiterAlgorithmSupport;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Min;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.aha.tech.core.support.LimiterAlgorithmSupport.getHeaders;

/**
 * @Author: luweihong
 * @Date: 2019/3/20
 *
 *  ip限流器
 */
@Primary
@Component("ipRateLimiter")
public class IpRateLimiter extends AbstractRateLimiter<IpRateLimiter.Config> implements ApplicationContextAware {

    public static final String CONFIGURATION_PROPERTY_NAME = "redis-rate-limiter";

    private Log log = LogFactory.getLog(getClass());

    private ReactiveRedisTemplate<String, String> redisTemplate;

    private RedisScript<List<Long>> script;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    public IpRateLimiter(ReactiveRedisTemplate<String, String> redisTemplate,
                         RedisScript<List<Long>> script, Validator validator) {
        super(Config.class, CONFIGURATION_PROPERTY_NAME, validator);
        this.redisTemplate = redisTemplate;
        this.script = script;
        initialized.compareAndSet(false, true);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    @Value("${ip.ratelimiter.replenish.rate:2}")
    private Integer replenishRate;

    @Value("${ip.ratelimiter.burst.capacity:16}")
    private Integer burstCapacity;

    @Autowired
    private LimiterAlgorithmSupport limiterAlgorithmSupport;

    /**
     * 重写限流逻辑
     * @param routeId
     * @param id
     * @return
     */
    @Override
    public Mono<RateLimiter.Response> isAllowed(String routeId, String id) {
        if (!this.initialized.get()) {
            throw new IllegalStateException("ip限流器没有初始化完毕");
        }

        try {
            List<String> keys = getKeys(id);
            return limiterAlgorithmSupport.execute(keys, replenishRate, burstCapacity, redisTemplate, script);
        } catch (Exception e) {
            log.error("执行ip限流算法出现异常", e);
        }

        return Mono.just(new Response(true, getHeaders(replenishRate, burstCapacity, -1L)));
    }

    /**
     * 重写插入的2个redis key 名称
     * @param id
     * @return
     */
    static List<String> getKeys(String id) {
        String prefix = "ip_rate_limiter.{" + id;
        String tokenKey = prefix + "}.tokens";
        String timestampKey = prefix + "}.timestamp";
        return Arrays.asList(tokenKey, timestampKey);
    }


    @Validated
    public static class Config {
        @Min(1)
        private int replenishRate;
        @Min(1)
        private int burstCapacity = 1;

        public int getReplenishRate() {
            return replenishRate;
        }

        public Config setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
            return this;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public Config setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
            return this;
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
        }
    }
}