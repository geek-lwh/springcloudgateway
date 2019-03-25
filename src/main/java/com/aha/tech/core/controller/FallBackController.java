package com.aha.tech.core.controller;

import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.model.vo.HystrixDataVo;
import com.aha.tech.core.model.vo.ResponseVo;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_ORIGINAL_URL_PATH_ATTR;
import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_REWRITE_PATH_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/3/12
 *
 * hystrix容错类
 */
@RestController
public class FallBackController {

    private static final String HYSTRIX_ERROR_MESSAGE_PREFIX = "接口熔断";

    /**
     * 降级策略
     * @param serverWebExchange
     * @return
     */
    @RequestMapping(value = "/fallback", method = RequestMethod.GET)
    public Mono<ResponseVo> fallBack(ServerWebExchange serverWebExchange) {
        Object c = serverWebExchange.getAttributes().get(ServerWebExchangeUtils.HYSTRIX_EXECUTION_EXCEPTION_ATTR);
        if (c == null) {
            ResponseVo responseVo = ResponseVo.defaultFailureResponseVo();
            responseVo.setMessage(HYSTRIX_ERROR_MESSAGE_PREFIX);
            return Mono.just(responseVo);
        }

        Throwable executionException = (Throwable) c;
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

        String originalUrlPath = serverWebExchange.getAttributes().get(GATEWAY_ORIGINAL_URL_PATH_ATTR).toString();
        hystrixDataVo.setOriginalUrlPath(originalUrlPath);

        String uri = serverWebExchange.getAttributes().get(GATEWAY_REQUEST_REWRITE_PATH_ATTR).toString();
        hystrixDataVo.setUri(uri);

        responseVo.setData(hystrixDataVo);

        return responseVo;
    }
}
