//package com.aha.tech.core.filters.global;
//
//import com.aha.tech.core.service.AuthorizationService;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import javax.annotation.Resource;
//
//import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_URL_WHITE_LIST_ATTR;
//import static com.aha.tech.core.constant.FilterProcessOrderedConstant.WHITE_LIST_REQUEST_FILTER;
//
///**
// * @Author: luweihong
// * @Date: 2019/5/8
// */
//@Component
//public class WhiteListFilter implements GlobalFilter,Ordered {
//
//    @Resource
//    private AuthorizationService httpAuthorizationService;
//
//    @Override
//    public int getOrder() {
//        return WHITE_LIST_REQUEST_FILTER;
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        String rawPath = exchange.getRequest().getURI().getRawPath();
//        Boolean isWhiteList = httpAuthorizationService.isWhiteList(rawPath);
//
//        exchange.getAttributes().put(GATEWAY_URL_WHITE_LIST_ATTR,isWhiteList);
//
//        return chain.filter(exchange);
//    }
//
//
//}
