package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.dto.RequestAddParamsDto;
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

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_ADD_PARAMS_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_REQUEST_BODY_FILTER_ORDER;
import static com.aha.tech.core.support.WriteResponseSupport.writeNpeParamsResponse;

/**
 * @Author: luweihong
 * @Date: 2019/3/27
 */
@Component
public class ModifyRequestBodyFilter implements GlobalFilter, Ordered {

    @Resource
    private OverwriteParamService httpOverwriteParamService;

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

        Object obj = exchange.getAttributes().get(GATEWAY_REQUEST_ADD_PARAMS_ATTR);
        if (obj == null) {
            logger.error("缺少需要在网关添加的参数");
            return Mono.defer(() -> writeNpeParamsResponse(exchange));
        }

        RequestAddParamsDto requestAddParamsDto = (RequestAddParamsDto) obj;
        return httpOverwriteParamService.modifyRequestBody(requestAddParamsDto, chain, exchange);
    }

}
