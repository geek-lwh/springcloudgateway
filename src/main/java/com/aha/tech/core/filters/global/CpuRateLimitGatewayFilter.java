package com.aha.tech.core.filters.global;

import com.aha.tech.commons.response.RpcResponse;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
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
 * 判断当前的cpu使用率,进行限流
 */
@Component
public class CpuRateLimitGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CpuRateLimitGatewayFilter.class);

    @Autowired
    private MetricsEndpoint metricsEndpoint;

    // java metrics 系统cpu指标
    private static final String METRIC_NAME = "system.cpu.usage";

    private static final String CPU_OVERLOAD_ERROR_MSG = "cpu负载过高,限制访问";

    // cpu使用率
    private static final double MAX_USAGE = 0.850D;

    @Override
    public int getOrder() {
        return GLOBAL_CPU_RATE_LIMITER_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Double systemCpuUsage = metricsEndpoint.metric(METRIC_NAME, null)
                .getMeasurements()
                .stream()
                .filter(Objects::nonNull)
                .findFirst()
                .map(MetricsEndpoint.Sample::getValue)
                .filter(Double::isFinite)
                .orElse(0.0D);

        boolean ok = systemCpuUsage < MAX_USAGE;
        if (ok) {
            return chain.filter(exchange);
        }

        logger.error("cpu 负载过高当前值: {} 阈值 : {}", systemCpuUsage, MAX_USAGE);

        return Mono.defer(() -> writeCpuOverloadError(exchange));
    }

    /**
     * 写入cpu负载过高的错误信息
     * @param exchange
     * @return
     */
    private Mono<Void> writeCpuOverloadError(ServerWebExchange exchange) {
        setResponseStatus(exchange, HttpStatus.TOO_MANY_REQUESTS);
        final ServerHttpResponse resp = exchange.getResponse();
        RpcResponse rpcResponse = new RpcResponse(HttpStatus.TOO_MANY_REQUESTS.value(), CPU_OVERLOAD_ERROR_MSG);
        byte[] bytes = JSON.toJSONString(rpcResponse).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
        return resp.writeWith(Flux.just(buffer));
    }

}
