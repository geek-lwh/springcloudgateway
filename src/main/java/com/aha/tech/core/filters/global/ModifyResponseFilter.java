package com.aha.tech.core.filters.global;

import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.model.entity.AccessLogEntity;
import com.aha.tech.core.service.RequestHandlerService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.*;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_RESPONSE_GATEWAY_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/4/1
 */
@Component
public class ModifyResponseFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyResponseFilter.class);

    @Resource
    private ThreadPoolTaskExecutor printAccessLogThreadPool;

    @Override
    public int getOrder() {
        return MODIFY_RESPONSE_GATEWAY_FILTER_ORDER;
    }

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始修改返回值过滤器");

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            // httpRequestHandlerService.modifyResponseBody(exchange,response);
            httpRequestHandlerService.modifyResponseHeader(response.getHeaders());
            CompletableFuture.runAsync(() -> printAccessLog(exchange, response.getStatusCode().value()),printAccessLogThreadPool);
//            printAccessLog(exchange, 1);
        }));


//        int httpStatus = serverHttpResponseDecorator.getStatusCode().value();
//        printAccessLog(exchange, 1);
//        CompletableFuture.runAsync(() -> printAccessLog(exchange, httpStatus), printAccessLogThreadPool);
//        return chain.filter(exchange.mutate().response(serverHttpResponseDecorator).build());
    }

    /**
     * 打印日志
     * @param exchange
     * @param httpStatus
     */
    private void printAccessLog(ServerWebExchange exchange, int httpStatus) {
        Map<String, Object> attributes = exchange.getAttributes();
        AccessLogEntity accessLogEntity = new AccessLogEntity();
        accessLogEntity.setRequestId(attributes.getOrDefault(ACCESS_REQUEST_ID_ATTR, Strings.EMPTY).toString());
        accessLogEntity.setRemoteIp(attributes.getOrDefault(ACCESS_REMOTE_IP_ATTR, Strings.EMPTY).toString());
        Long requestTime = (Long) attributes.getOrDefault(ACCESS_REQUEST_TIME_ATTR, System.currentTimeMillis());
        accessLogEntity.setRequestTime(DateUtil.dateByDefaultFormat(new Date(requestTime)));
        Long endTime = System.currentTimeMillis();
        Long cost = endTime - requestTime;
        accessLogEntity.setCost(cost);
        accessLogEntity.setUserName(attributes.getOrDefault(ACCESS_USER_NAME_ATTR, Strings.EMPTY).toString());
        accessLogEntity.setOriginalPath(attributes.getOrDefault(ACCESS_LOG_ORIGINAL_URL_PATH_ATTR, Strings.EMPTY).toString());
        accessLogEntity.setCookie(attributes.getOrDefault(ACCESS_LOG_COOKIE_ATTR, Strings.EMPTY).toString());
        accessLogEntity.setHttpStatus(httpStatus);

        logger.info("访问日志: {}", accessLogEntity);
//        accessLogEntity.setCost(attributes.getOrDefault(ACCESS_LOG_COOKIE_ATTR, Strings.EMPTY).toString());
    }

}
