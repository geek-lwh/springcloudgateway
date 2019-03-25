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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
 * 自定义redis 限流的逻辑
 */
@Component
public class QpsRateLimiter extends AbstractRateLimiter<QpsRateLimiter.Config> implements ApplicationContextAware {

    public static final String CONFIGURATION_PROPERTY_NAME = "redis-rate-limiter";

    public static final String REDIS_SCRIPT_NAME = "redisRequestRateLimiterScript";

    private Log log = LogFactory.getLog(getClass());

    private ReactiveRedisTemplate<String, String> redisTemplate;

    private RedisScript<List<Long>> script;

    private AtomicBoolean initialized = new AtomicBoolean(false);


    @Value("${qps.ratelimiter.replenish.rate:50}")
    private Integer replenishRate;

    @Value("${qps.ratelimiter.burst.capacity:2000}")
    private Integer burstCapacity;

    @Autowired
    private LimiterAlgorithmSupport limiterAlgorithmSupport;

    public QpsRateLimiter(ReactiveRedisTemplate<String, String> redisTemplate,
                          RedisScript<List<Long>> script, Validator validator) {
        super(Config.class, CONFIGURATION_PROPERTY_NAME, validator);
        this.redisTemplate = redisTemplate;
        this.script = script;
        initialized.compareAndSet(false, true);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    /**
     * 重写限流逻辑
     * @param routeId
     * @param id
     * @return
     */
    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        if (!this.initialized.get()) {
            throw new IllegalStateException("qps限流器没有初始化完毕");
        }

        try {
            List<String> keys = getKeys(id);
            return limiterAlgorithmSupport.execute(keys, replenishRate, burstCapacity, redisTemplate, script);
        } catch (Exception e) {
            log.error("执行qps限流算法出现异常", e);
        }

        return Mono.just(new Response(true, getHeaders(replenishRate, burstCapacity, -1L)));
    }

    /**
     * 重写插入的2个redis key 名称
     * @param id
     * @return
     */
    static List<String> getKeys(String id) {
        String prefix = "qps_rate_limiter.{" + id;
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