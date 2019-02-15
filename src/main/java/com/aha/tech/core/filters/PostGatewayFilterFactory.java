//package com.aha.tech.core.filters;
//
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import reactor.core.publisher.Mono;
//
///**
// * @Author: luweihong
// * @Date: 2019/2/14
// */
//public class PostGatewayFilterFactory extends AbstractGatewayFilterFactory<PostGatewayFilterFactory.Config> {
//
//    @Override
//    public GatewayFilter apply(PostGatewayFilterFactory.Config config) {
//        return (exchange, chain) -> {
//            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
//                ServerHttpResponse response = exchange.getResponse();
//                //Manipulate the response in some way
//                System.out.println("hahah response is " + response);
//            }));
//        };
//    }
//
//    public static class Config {
//
//    }
//}
