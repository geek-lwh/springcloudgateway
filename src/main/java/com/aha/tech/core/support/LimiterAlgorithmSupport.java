package com.aha.tech.core.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: luweihong
 * @Date: 2019/3/25
 *
 * 限流算法辅助类
 */
@Component
public class LimiterAlgorithmSupport {

    private static final Logger logger = LoggerFactory.getLogger(LimiterAlgorithmSupport.class);

    // 每次减去1个
    private static final String BUCKLE_VALUE = "1";

    // 剩余多少流量
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    // 限流速率
    public static final String REPLENISH_RATE_HEADER = "X-RateLimit-Replenish-Rate";

    // 总流量
    public static final String BURST_CAPACITY_HEADER = "X-RateLimit-Burst-Capacity";


    /**
     * 执行全局限流算法
     * @param keys
     * @param replenishRate
     * @param burstCapacity
     * @param redisTemplate
     * @param script
     * @return
     */
    public Mono<RateLimiter.Response> execute(List<String> keys, int replenishRate, int burstCapacity, ReactiveRedisTemplate redisTemplate, RedisScript<List<Long>> script) {
        String time = String.valueOf(Instant.now().getEpochSecond());
        List<String> scriptArgs = Arrays.asList(String.valueOf(replenishRate), String.valueOf(burstCapacity), time, BUCKLE_VALUE);
        Flux<List<Long>> flux = redisTemplate.execute(script, keys, scriptArgs);
        return flux.onErrorResume(throwable -> Flux.just(Arrays.asList(1L, -1L)))
                .reduce(new ArrayList<Long>(), (longs, l) -> {
                    longs.addAll(l);
                    return longs;
                }).map(results -> {
                    boolean allowed = results.get(0) == 1L;
                    Long tokensLeft = results.get(1);
                    HashMap<String, String> responseHeader = getHeaders(replenishRate, burstCapacity, tokensLeft);
                    RateLimiter.Response response = new RateLimiter.Response(allowed, responseHeader);
                    if (!allowed) {
                        logger.warn("ip限流生效 : {}", responseHeader);
                    }
                    return response;
                });
    }

    /**
     * 限流后设置返回头信息
     *
     * @param replenishRate
     * @param burstCapacity
     * @param tokensLeft
     * @return
     */
    public static HashMap<String, String> getHeaders(Integer replenishRate, Integer burstCapacity, Long tokensLeft) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put(REMAINING_HEADER, tokensLeft.toString());
        headers.put(REPLENISH_RATE_HEADER, String.valueOf(replenishRate));
        headers.put(BURST_CAPACITY_HEADER, String.valueOf(burstCapacity));
        return headers;
    }
}
