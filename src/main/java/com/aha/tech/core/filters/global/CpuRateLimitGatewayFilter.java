package com.aha.tech.core.filters.global;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.support.WriteResponseSupport;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.aha.tech.core.constant.GatewayFilterProcessOrderedConstant.GLOBAL_CPU_RATE_LIMITER_FILTER_ORDER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/3/19
 *
 * 针对平均1分钟系统资源利用率进行限流
 *
 */
@Component
public class CpuRateLimitGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CpuRateLimitGatewayFilter.class);

    @Autowired
    private MetricsEndpoint metricsEndpoint;

    @Value("${system.load.average.1m.ratelimiter.enable:false}")
    private Boolean isEnable;

    @Value("${system.load.average.1m.ratelimiter.overload:0.85}")
    private double systemLoadAverage;

    // java metrics 系统cpu指标
    private static final String METRIC_NAME = "system.load.average.1m";

    private static final String CPU_OVERLOAD_ERROR_MSG = "cpu负载过高,限制访问";

    @Override
    public int getOrder() {
        return GLOBAL_CPU_RATE_LIMITER_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isEnable) {
            return chain.filter(exchange);
        }

        Double systemCpuUsage = metricsEndpoint.metric(METRIC_NAME, null)
                .getMeasurements()
                .stream()
                .filter(Objects::nonNull)
                .findFirst()
                .map(MetricsEndpoint.Sample::getValue)
                .filter(Double::isFinite)
                .orElse(0.0D);

        boolean ok = systemCpuUsage < systemLoadAverage;
        if (ok) {
            return chain.filter(exchange);
        }

        logger.error("cpu 负载过高当前值: {} 平均1分钟系统利用率设定的阈值 : {}", systemCpuUsage, systemCpuUsage);

        ResponseVo responseVo = ResponseVo.defaultFailureResponseVo();
        return Mono.defer(() -> WriteResponseSupport.write(exchange,responseVo,HttpStatus.TOO_MANY_REQUESTS));
    }

}
