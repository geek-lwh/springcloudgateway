//package com.aha.tech.core.filters.normal;
//
//import com.aha.tech.commons.response.RpcResponse;
//import com.aha.tech.core.limiter.IpRateLimiter;
//import com.alibaba.fastjson.JSON;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
//import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
//import org.springframework.cloud.gateway.route.Route;
//import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
//import org.springframework.core.Ordered;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import javax.annotation.Resource;
//import java.nio.charset.StandardCharsets;
//import java.util.concurrent.ExecutionException;
//
//import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;
//
///**
// * @Author: luweihong
// * @Date: 2019/3/20
// */
//@Component
//public class RateLimiterGatewayFilterFactory implements GlobalFilter, Ordered {
//
//    private static final Logger logger = LoggerFactory.getLogger(RateLimiterGatewayFilterFactory.class);
//
//    public static final String IP_RATE_LIMITER_ERROR_MSG = "ip限流拦截溢出请求";
//
//    @Resource
//    private KeyResolver ipKeyReskolver;
//
//    @Resource
//    private IpRateLimiter ipRateLimiter;
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        logger.info("开始执行ip限流过滤器");
//        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
//        String key = ipKeyResolver.resolve(exchange).block();
//        Mono<RateLimiter.Response> rateLimiterAllowed = ipRateLimiter.isAllowed(route.getId(), key);
//        try {
//            if(rateLimiterAllowed.toFuture().get().isAllowed()){
//                return chain.filter(exchange);
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
//
//        logger.error("没有通过ip限流");
//        return Mono.defer(()-> writeIpRateLimiterError(exchange));
//    }
//
//    /**
//     * 根据错误code码返回信息
//     * @param exchange
//     * @return
//     */
//    private Mono<Void> writeIpRateLimiterError(ServerWebExchange exchange) {
//        logger.error("触发全局ip限流访问控制");
//        setResponseStatus(exchange, HttpStatus.TOO_MANY_REQUESTS);
//        final ServerHttpResponse resp = exchange.getResponse();
//        RpcResponse rpcResponse = RpcResponse.defaultFailureResponse();
//        rpcResponse.setMessage(IP_RATE_LIMITER_ERROR_MSG);
//        byte[] bytes = JSON.toJSONString(rpcResponse).getBytes(StandardCharsets.UTF_8);
//        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
//        resp.getHeaders().setContentLength(buffer.readableByteCount());
//        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
//        return resp.writeWith(Flux.just(buffer));
//    }
//
//    @Override
//    public int getOrder() {
//        return 0;
//    }
//
//
//}
