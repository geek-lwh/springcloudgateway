package com.aha.tech.core.filters.global;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.constant.FilterOrderedConstant;
import com.aha.tech.core.model.entity.RouteEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.URL_IN_WHITE_LIST;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 *
 * 调用授权系统解析用户主键
 * 修改body请求体
 */
@Component
public class PreHandlerGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(PreHandlerGatewayFilter.class);

    private static final String PUBLIC_TEXT = "public";

    private static final int SKIP_FIRST_PART = 1;

    private static final int SKIP_SECOND_PART = 2;

    @Autowired
    private Map<String, List<String>> whiteListMap;

    @Resource
    private Map<String, RouteEntity> routeEntityMap;

    @Override
    public int getOrder() {
        return FilterOrderedConstant.GLOBAL_REWRITE_REQUEST_PATH_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("重写请求路径");
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        URI uri = serverHttpRequest.getURI();
        logger.info("before rewrite url is : {}", uri);
        String path = uri.getRawPath();

        ServerHttpRequest newRequest = rewriteRequestPath(path, serverHttpRequest, exchange);

        // 根据attributes 判断是否执行对应请求
        logger.info("after rewrite url is : {}", newRequest.getURI().getRawPath());
        return chain.filter(exchange.mutate().request(newRequest).build());
    }


    /**
     * 重写请求路径
     * @param path
     * @param serverHttpRequest
     * @return
     */
    private ServerHttpRequest rewriteRequestPath(String path, ServerHttpRequest serverHttpRequest, ServerWebExchange exchange) {


        Stream<String> realUrlStream = Arrays.stream(org.springframework.util.StringUtils.tokenizeToStringArray(path, Separator.SLASH_MARK)).skip(SKIP_FIRST_PART);

        String subUrl = realUrlStream.collect(Collectors.joining(Separator.SLASH_MARK));
        String id = StringUtils.substringBefore(subUrl, Separator.SLASH_MARK);
        if (!routeEntityMap.containsKey(id)) {
            logger.error("没有匹配的路由地址 : {}", path);
            return serverHttpRequest;
        }

        RouteEntity routeEntity = routeEntityMap.get(id);
        String rewritePath = new StringBuilder()
                .append(routeEntity.getContextPath())
                .append(Separator.SLASH_MARK)
                .append(subUrl).toString();

        ServerHttpRequest newRequest = serverHttpRequest.mutate()
                .path(rewritePath)
                .build();

        setExchangeAttributes(id, subUrl, exchange, newRequest);
        return newRequest;
    }

    /**
     * 设置exchange attributes
     * @param exchange
     * @param newRequest
     */
    private void setExchangeAttributes(String id, String path, ServerWebExchange exchange, ServerHttpRequest newRequest) {
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, newRequest.getURI());
        exchange.getAttributes().put(URL_IN_WHITE_LIST, Boolean.FALSE);

        List<String> whiteList = whiteListMap.containsKey(id) ? whiteListMap.get(id) : Collections.EMPTY_LIST;
        if (whiteList.contains(path)) {
            exchange.getAttributes().put(URL_IN_WHITE_LIST, Boolean.TRUE);
        }
    }

}
