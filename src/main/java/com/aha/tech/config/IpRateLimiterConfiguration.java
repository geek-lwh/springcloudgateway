package com.aha.tech.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: luweihong
 * @Date: 2019/3/20
 */
@Configuration
public class IpRateLimiterConfiguration {

    @Value("${ip.ratelimiter.default.replenish.rate:50}")
    private Integer replenishRate;

    @Value("${ip.ratelimiter.default.burst.capacity:2000}")
    private Integer burstCapacity;

    public IpRateLimiterConfiguration(){
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