package com.aha.tech.core.filters.global;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.model.enums.WebClientTypeEnum;
import com.aha.tech.core.service.RequestHandlerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.aha.tech.core.constant.FilterOrderedConstant.GLOBAL_ADD_REQUEST_HEADER_GATEWAY_FILTER_ORDER;
import static com.aha.tech.core.constant.HeaderFieldConstant.*;
import static com.aha.tech.core.tools.BeanUtil.copyMultiValueMap;

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 *
 * 修改请求头网关过滤器
 */
@Component
public class ModifyRequestHeaderGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyRequestHeaderGatewayFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return GLOBAL_ADD_REQUEST_HEADER_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始进行修改请求头网关过滤器");
        ServerHttpRequest newRequest = httpRequestHandlerService.modifyRequestHeaders(exchange);

        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
        return chain.filter(newExchange);
    }

}
