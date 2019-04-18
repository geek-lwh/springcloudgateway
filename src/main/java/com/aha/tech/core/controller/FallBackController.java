package com.aha.tech.core.controller;

import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.model.vo.HystrixDataVo;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.AccessLogService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR;
import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_REWRITE_PATH_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/3/12
 *
 * hystrix容错类
 */
@RestController
public class FallBackController {

    private static final Logger logger = LoggerFactory.getLogger(FallBackController.class);

    private static final String HYSTRIX_ERROR_MESSAGE_PREFIX = "接口熔断,未捕获到具体错误!";

    @Resource
    private AccessLogService httpAccessLogService;

    /**
     * 降级策略
     * @param serverWebExchange
     * @return
     */
    @RequestMapping(value = "/fallback", method = RequestMethod.GET)
    public Mono<ResponseVo> fallBack(ServerWebExchange serverWebExchange) {
        Object c = serverWebExchange.getAttributes().get(ServerWebExchangeUtils.HYSTRIX_EXECUTION_EXCEPTION_ATTR);
        if (c == null) {
            httpAccessLogService.printWhenError(serverWebExchange, "未捕获到hystrix异常!");
            ResponseVo responseVo = ResponseVo.defaultFailureResponseVo();
            responseVo.setMessage(HYSTRIX_ERROR_MESSAGE_PREFIX);
            return Mono.just(responseVo);
        }

        Throwable executionException = (Throwable) c;
        Exception e = (Exception) executionException;
        String errorMsg = String.format("出现额外异常,触发hystrix 降级策略,异常信息:%s", e.getMessage());
        httpAccessLogService.printWhenError(serverWebExchange, errorMsg);

        ResponseVo responseVo = buildHystrixResponse(executionException, serverWebExchange);

        return Mono.just(responseVo);
    }

    /**
     * 构建带有hystrix信息的data
     * @param executionException
     * @param serverWebExchange
     * @return
     */
    private ResponseVo<HystrixDataVo> buildHystrixResponse(Throwable executionException, ServerWebExchange serverWebExchange) {
        ResponseVo responseVo = ResponseVo.defaultFailureResponseVo();
        responseVo.setMessage(HYSTRIX_ERROR_MESSAGE_PREFIX);

        HystrixDataVo hystrixDataVo = new HystrixDataVo();
        String errorMessage = String.format("%s:%s", HYSTRIX_ERROR_MESSAGE_PREFIX, executionException.toString());
        hystrixDataVo.setMessage(errorMessage);

        hystrixDataVo.setTime(DateUtil.currentDateByDefaultFormat());

        String originalUrlPath = serverWebExchange.getAttributes().getOrDefault(GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR, Strings.EMPTY).toString();
        hystrixDataVo.setOriginalUrlPath(originalUrlPath);

        String uri = serverWebExchange.getAttributes().getOrDefault(GATEWAY_REQUEST_REWRITE_PATH_ATTR, Strings.EMPTY).toString();
        hystrixDataVo.setUri(uri);

        responseVo.setData(hystrixDataVo);

        return responseVo;
    }
}
