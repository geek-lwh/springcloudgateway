package com.aha.tech.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import javax.validation.constraints.Min;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: luweihong
 * @Date: 2019/3/20
 *
 * 自定义redis 限流的逻辑
 */
@Component
public class IpRateLimiter extends AbstractRateLimiter<IpRateLimiter.Config> implements ApplicationContextAware {

    public static final String CONFIGURATION_PROPERTY_NAME = "redis-rate-limiter";
    public static final String REDIS_SCRIPT_NAME = "redisRequestRateLimiterScript";
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String REPLENISH_RATE_HEADER = "X-RateLimit-Replenish-Rate";
    public static final String BURST_CAPACITY_HEADER = "X-RateLimit-Burst-Capacity";

    private Log log = LogFactory.getLog(getClass());

    private ReactiveRedisTemplate<String, String> redisTemplate;
    private RedisScript<List<Long>> script;
    private AtomicBoolean initialized = new AtomicBoolean(false);
//    private Config defaultConfig;

    // configuration properties
    /** Whether or not to include headers containing rate limiter information, defaults to true. */
//    private boolean includeHeaders = true;

    /** The name of the header that returns number of remaining requests during the current second. */
    private String remainingHeader = REMAINING_HEADER;

    /** The name of the header that returns the replenish rate configuration. */
    private String replenishRateHeader = REPLENISH_RATE_HEADER;

    /** The name of the header that returns the burst capacity configuration. */
    private String burstCapacityHeader = BURST_CAPACITY_HEADER;

    public IpRateLimiter(ReactiveRedisTemplate<String, String> redisTemplate,
                         RedisScript<List<Long>> script, Validator validator) {
        super(Config.class , CONFIGURATION_PROPERTY_NAME , validator);
        this.redisTemplate = redisTemplate;
        this.script = script;
        initialized.compareAndSet(false,true);
    }

    @Resource
    private IpRateLimiterConfiguration ipRateLimiterConfiguration;

    /**
     * 重写限流逻辑
     * @param routeId
     * @param id
     * @return
     */
    @Override
    public Mono<RateLimiter.Response> isAllowed(String routeId, String id) {
        if (!this.initialized.get()) {
            throw new IllegalStateException("RedisRateLimiter is not initialized");
        }

        if (ObjectUtils.isEmpty(rateLimiterConf) ){
            throw new IllegalArgumentException("No Configuration found for route " + routeId);
        }


        // How many requests per second do you want a user to be allowed to do?
        int replenishRate = ipRateLimiterConfiguration.getReplenishRate();

        // How much bursting do you want to allow?
        int burstCapacity = ipRateLimiterConfiguration.getBurstCapacity();

        try {
            List<String> keys = getKeys(id);

            // The arguments to the LUA script. time() returns unixtime in seconds.
            List<String> scriptArgs = Arrays.asList(replenishRate + "", burstCapacity + "",
                    Instant.now().getEpochSecond() + "", "1");
            // allowed, tokens_left = redis.eval(SCRIPT, keys, args)
            Flux<List<Long>> flux = this.redisTemplate.execute(this.script, keys, scriptArgs);
            // .log("redisratelimiter", Level.FINER);
            return flux.onErrorResume(throwable -> Flux.just(Arrays.asList(1L, -1L)))
                    .reduce(new ArrayList<Long>(), (longs, l) -> {
                        longs.addAll(l);
                        return longs;
                    }) .map(results -> {
                        boolean allowed = results.get(0) == 1L;
                        Long tokensLeft = results.get(1);

                        Response response = new Response(allowed, getHeaders(replenishRate , burstCapacity, tokensLeft));

                        if (log.isDebugEnabled()) {
                            log.debug("response: " + response);
                        }
                        return response;
                    });
        }
        catch (Exception e) {
            /*
             * We don't want a hard dependency on Redis to allow traffic. Make sure to set
             * an alert so you know if this is happening too much. Stripe's observed
             * failure rate is 0.01%.
             */
            log.error("Error determining if user allowed from redis", e);
        }

//        return Mono.just(new Response(true, getHeaders(routeConfig, -1L)));

        return Mono.just(new RateLimiter.Response(true, getHeaders(replenishRate , burstCapacity , -1L)));
    }

    private IpRateLimiterConfiguration rateLimiterConf;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.rateLimiterConf = applicationContext.getBean(IpRateLimiterConfiguration.class);
    }

    public HashMap<String, String> getHeaders(Integer replenishRate, Integer burstCapacity , Long tokensLeft) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put(this.remainingHeader, tokensLeft.toString());
        headers.put(this.replenishRateHeader, String.valueOf(replenishRate));
        headers.put(this.burstCapacityHeader, String.valueOf(burstCapacity));
        return headers;
    }

    /**
     * 重写插入的2个redis key 名称
     * @param id
     * @return
     */
    static List<String> getKeys(String id) {
        // use `{}` around keys to use Redis Key hash tags
        // this allows for using redis cluster

        // Make a unique key per user.
        //此处可以自定义redis前缀信息
        String prefix = "ip_rate_limiter.{" + id;

        // You need two Redis keys for Token Bucket.
        String tokenKey = prefix + "}.tokens";
        String timestampKey = prefix + "}.timestamp";
        return Arrays.asList(tokenKey, timestampKey);
    }


    @Validated
    public static class Config{
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
            return "Config{" +
                    "replenishRate=" + replenishRate +
                    ", burstCapacity=" + burstCapacity +
                    '}';
        }
    }
}