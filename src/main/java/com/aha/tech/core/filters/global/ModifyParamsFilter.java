package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.service.OverwriteParamService;
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

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_ADD_PARAMS_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_PARAMS_FILTER_ORDER;
import static com.aha.tech.core.support.WriteResponseSupport.writeNpeParamsResponse;

/**
 * @Author: luweihong
 * @Date: 2019/3/27
 *
 * 修改GET,DELETE 等请求的参数
 */
@Component
public class ModifyParamsFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyParamsFilter.class);

    @Resource
    private OverwriteParamService httpOverwriteParamService;

    @Override
    public int getOrder() {
        return MODIFY_PARAMS_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始进入修改GET|POST请求参数过滤器");

        Object obj = exchange.getAttributes().get(GATEWAY_REQUEST_ADD_PARAMS_ATTR);
        if (obj == null) {
            logger.error("缺少需要在网关添加的参数");
            return Mono.defer(() -> writeNpeParamsResponse(exchange));
        }

        RequestAddParamsDto requestAddParamsDto = (RequestAddParamsDto) obj;
        HttpMethod httpMethod = exchange.getRequest().getMethod();
        if (httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
            return httpOverwriteParamService.modifyRequestBody(requestAddParamsDto, chain, exchange);
        }

        if (httpMethod.equals(HttpMethod.GET) || httpMethod.equals(HttpMethod.DELETE)) {
            ServerHttpRequest serverHttpRequest = exchange.getRequest();
            URI uri = serverHttpRequest.getURI();
            URI newUri = httpOverwriteParamService.modifyQueryParams(requestAddParamsDto, uri);
            ServerHttpRequest newRequest = exchange.getRequest().mutate().uri(newUri).build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        }

        return chain.filter(exchange);
    }

}
