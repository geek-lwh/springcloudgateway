package com.aha.tech.core.filters.global;

import com.aha.tech.core.constant.FilterProcessOrderedConstant;
import com.aha.tech.core.exception.GatewayException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.WriteResponseSupport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR;

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

        String path = exchange.getAttribute(GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR);
        try {
            httpRequestHandlerService.authorize(exchange);
        } catch (GatewayException ge) {
            return Mono.defer(() -> writeWithGatewayError(exchange, path, ge));
        }

        return chain.filter(exchange);
    }

    /**
     * 根据错误code码返回信息
     * @param exchange
     * @param e
     * @return
     */
    private Mono<Void> writeWithGatewayError(ServerWebExchange exchange, String path, GatewayException e) {
        ResponseVo rpcResponse = new ResponseVo(e.getCode(), e.getMessage());
        logger.warn("访问路径: {} 失败,原因 : 权限不足,返回值", path, e, rpcResponse);
        return WriteResponseSupport.write(exchange, rpcResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 非业务异常捕获
     * @param exchange
     * @param e
     * @return
     */
    @Deprecated
    private Mono<Void> writeWithError(ServerWebExchange exchange, Exception e) {
        logger.error("权限校验过滤器出现异常", e);
        ResponseVo rpcResponse = ResponseVo.defaultFailureResponseVo();
        String message = e.getMessage();
        if (StringUtils.isBlank(message) && e.getCause() != null) {
            message = e.getCause().toString();
        } else {
            message = e.getClass().toString();
        }

        rpcResponse.setMessage(message);
        return WriteResponseSupport.write(exchange, rpcResponse, HttpStatus.BAD_GATEWAY);
    }

}
