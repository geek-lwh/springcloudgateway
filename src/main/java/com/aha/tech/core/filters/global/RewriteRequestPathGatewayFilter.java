package com.aha.tech.core.filters.global;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.constant.FilterOrderedConstant;
import com.aha.tech.core.model.entity.RouteEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 *
 * 调用授权系统解析用户主键
 * 修改body请求体
 */
@Component
public class RewriteRequestPathGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RewriteRequestPathGatewayFilter.class);

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
        logger.info("uri is : {}", uri);
        String path = uri.getRawPath();

        ServerHttpRequest newRequest = rewriteRequestPath(path, serverHttpRequest);

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, newRequest.getURI());
        return chain.filter(exchange.mutate().request(newRequest).build());
    }


    /**
     * 重写请求路径
     * @param path
     * @param serverHttpRequest
     * @return
     */
    private ServerHttpRequest rewriteRequestPath(String path, ServerHttpRequest serverHttpRequest) {
        String[] arr = StringUtils.split(path, Separator.SLASH_MARK);
        String mappingKey = arr[1];
        RouteEntity routeEntity = routeEntityMap.containsKey(mappingKey) ? routeEntityMap.get(mappingKey) : null;
        if (routeEntity == null) {
            logger.error("no mapping handler url : {}", path);
            // throw error
        }

//        StringBuilder rewritePath = new StringBuilder(routeEntity.getContextPath()).append(Separator.SLASH_MARK);
//        int index = 1;
//        for (int i = index; i < arr.length; i++) {
//            rewritePath.append(arr[i]).append(Separator.SLASH_MARK);
//        }
        String rewritePath = path.replaceAll(arr[0],routeEntity.getContextPath());
//        String rewritePath = String.format("%s%s", routeEntity.getContextPath(), path);
        ServerHttpRequest newRequest = serverHttpRequest.mutate()
                .path(rewritePath)
                .build();

        return newRequest;
    }

}
