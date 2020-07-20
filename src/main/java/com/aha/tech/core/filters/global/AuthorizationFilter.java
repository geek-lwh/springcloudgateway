package com.aha.tech.core.filters.global;

import com.aha.tech.commons.constants.ResponseConstants;
import com.aha.tech.core.constant.FilterProcessOrderedConstant;
import com.aha.tech.core.model.entity.AuthenticationResultEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ResponseSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;

import static com.aha.tech.core.constant.HeaderFieldConstant.REQUEST_ID;
import static com.aha.tech.core.interceptor.FeignRequestInterceptor.TRACE_ID;

/**
 * @Author: luweihong
 * @Date: 2019/2/14
 *
 * 鉴权校验
 */
@Component
public class AuthorizationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return FilterProcessOrderedConstant.AUTH_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始执行权限校验网关过滤器");

        List<String> clientRequestId = exchange.getRequest().getHeaders().get(REQUEST_ID);
        if (!CollectionUtils.isEmpty(clientRequestId)) {
            MDC.put(TRACE_ID, clientRequestId.get(0));
        }

        AuthenticationResultEntity authenticationResultEntity = httpRequestHandlerService.authorize(exchange);
        Boolean isWhiteList = authenticationResultEntity.getWhiteList();
        if (isWhiteList) {
            return chain.filter(exchange);
        }

        Integer code = authenticationResultEntity.getCode();
        if (code.equals(ResponseConstants.SUCCESS)) {
            return chain.filter(exchange);
        }

        String message = authenticationResultEntity.getMessage();
        logger.warn("授权异常 : {}", message);
        return Mono.defer(() -> {
            ResponseVo rpcResponse = new ResponseVo(code, message);
            return ResponseSupport.write(exchange, HttpStatus.UNAUTHORIZED, rpcResponse);
        });
    }

}
