package com.aha.tech.core.filters.global;

import com.aha.tech.core.service.OverwriteParamService;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_USER_VO_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_REQUEST_BODY_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/3/27
 */
@Component
public class ModifyRequestBodyFilter implements GlobalFilter, Ordered {

    @Resource
    private OverwriteParamService overwriteParamService;

    private static final Logger logger = LoggerFactory.getLogger(ModifyRequestBodyFilter.class);

    @Override
    public int getOrder() {
        return MODIFY_REQUEST_BODY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始进入修改POST请求参数过滤器");
        HttpMethod httpMethod = exchange.getRequest().getMethod();
        if (httpMethod.equals(HttpMethod.GET) || httpMethod.equals(HttpMethod.DELETE)) {
            return chain.filter(exchange);
        }

        if (!exchange.getAttributes().containsKey(GATEWAY_USER_VO_ATTR)) {
            return chain.filter(exchange);
        }

        UserVo userVo = (UserVo) exchange.getAttributes().get(GATEWAY_USER_VO_ATTR);
        return overwriteParamService.modifyRequestBody(userVo, chain, exchange);
    }

}
