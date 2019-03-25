package com.aha.tech.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: luweihong
 * @Date: 2019/3/20
 *
 * 总流量限流
 */
@Configuration
public class QpsRateLimiterConfiguration {

    @Value("${qps.ratelimiter.replenish.rate:50}")
    private Integer replenishRate;

    @Value("${qps.ratelimiter.burst.capacity:2000}")
    private Integer burstCapacity;

    public QpsRateLimiterConfiguration(){
        super();
    }

    public Integer getReplenishRate() {
        return replenishRate;
    }

    public void setReplenishRate(Integer replenishRate) {
        this.replenishRate = replenishRate;
    }

    public Integer getBurstCapacity() {
        return burstCapacity;
    }

    public void setBurstCapacity(Integer burstCapacity) {
        this.burstCapacity = burstCapacity;
    }
}