package com.aha.tech.core.filters.global;

import com.aha.tech.core.service.OverwriteParamService;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_USER_VO_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_QUERY_PARAMS_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/3/27
 *
 * 修改GET,DELETE 等请求的参数
 */
@Component
public class ModifyQueryParamsFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyQueryParamsFilter.class);

    @Resource
    private OverwriteParamService overwriteParamService;

    @Override
    public int getOrder() {
        return MODIFY_QUERY_PARAMS_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始进入修改GET请求参数过滤器");

        HttpMethod httpMethod = exchange.getRequest().getMethod();
        if (httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
            return chain.filter(exchange);
        }

        if (!exchange.getAttributes().containsKey(GATEWAY_USER_VO_ATTR)) {
            // todo 返回
            return chain.filter(exchange);
        }

        Object obj = exchange.getAttributes().get(GATEWAY_USER_VO_ATTR);
        if (obj == null) {
            // todo npe 抛出
        }

        UserVo userVo = (UserVo) obj;

        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        URI uri = serverHttpRequest.getURI();
        URI newUri = overwriteParamService.modifyQueryParams(userVo, uri);
        ServerHttpRequest request = exchange.getRequest().mutate().uri(newUri).build();

        return chain.filter(exchange.mutate().request(request).build());
    }

}
