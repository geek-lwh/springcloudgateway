package com.aha.tech.core.support;

import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.AccessLogService;
import com.aha.tech.util.SpringContextUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/3/25
 *
 * 返回response信息辅助类
 */
public class WriteResponseSupport {

    private static final Logger logger = LoggerFactory.getLogger(WriteResponseSupport.class);

    /**
     * 写入response body
     * @param exchange
     * @param responseVo
     * @return
     */
    public static Mono<Void> shortCircuit(ServerWebExchange exchange, ResponseVo responseVo, String errorMsg) {
        logger.error("捕获异常 --> {}", errorMsg);
        final ServerHttpResponse resp = exchange.getResponse();
        byte[] bytes = JSON.toJSONString(responseVo).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
        resp.getHeaders().setContentLength(buffer.readableByteCount());
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        setResponseStatus(exchange, HttpStatus.OK);
        AccessLogService httpAccessLogService = (AccessLogService) SpringContextUtil.getBean("httpAccessLogService");
        httpAccessLogService.printWhenError(exchange, errorMsg);
        return resp.writeWith(Flux.just(buffer));
    }

}
