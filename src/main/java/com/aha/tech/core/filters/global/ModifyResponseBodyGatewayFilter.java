package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.ModifyResponseService;
import com.aha.tech.core.service.RequestHandlerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.aha.tech.core.constant.FilterOrderedConstant.GLOBAL_MODIFY_RESPONSE_BODY_GATEWAY_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 * 修改response body 网关过滤器
 */
@Component
public class ModifyResponseBodyGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyResponseBodyGatewayFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return GLOBAL_MODIFY_RESPONSE_BODY_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始执行修改返回体网关过滤器");
        ServerHttpResponseDecorator newResponse = httpRequestHandlerService.modifyResponseBody(exchange);

        ServerWebExchange newExchange = exchange.mutate().response(newResponse).build();

        return chain.filter(newExchange);
    }

}
