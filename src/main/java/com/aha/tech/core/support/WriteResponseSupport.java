package com.aha.tech.core.support;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.exception.GatewayException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
     * 根据异常值返回
     * @param exchange
     * @param gatewayException
     * @return
     */
    public static Mono<Void> writeError(ServerWebExchange exchange, GatewayException gatewayException) {
        final ServerHttpResponse resp = exchange.getResponse();
        ResponseVo responseVo = new ResponseVo(gatewayException.getCode(), gatewayException.getMessage());
        byte[] bytes = JSON.toJSONString(responseVo).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
        resp.getHeaders().setContentLength(buffer.readableByteCount());
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        setResponseStatus(exchange, HttpStatus.BAD_GATEWAY);
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        logger.error("{}", writeRequestInfo(serverHttpRequest));
        return resp.writeWith(Flux.just(buffer));
    }

    /**
     * 打印请求信息
     * @param serverHttpRequest
     * @return
     */
    public static String writeRequestInfo(ServerHttpRequest serverHttpRequest) {
        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
        StringBuilder sb = new StringBuilder("请求地址 = [");
        sb.append(serverHttpRequest.getURI()).append(System.lineSeparator());
        sb.append("]");
        sb.append("报头信息 = [");
        httpHeaders.forEach((key, value) -> {
            sb.append(key).append(Separator.EQUAL_SIGN_MARK).append(value);
            sb.append(Separator.COMMA_MARK);
        });

        sb.deleteCharAt(sb.length());
        sb.append("]");


        return sb.toString();
    }

    /**
     * 写入response body
     * @param exchange
     * @param responseVo
     * @param httpStatus
     * @return
     */
    public static Mono<Void> write(ServerWebExchange exchange, ResponseVo responseVo, HttpStatus httpStatus) {
        final ServerHttpResponse resp = exchange.getResponse();
        byte[] bytes = JSON.toJSONString(responseVo).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
        resp.getHeaders().setContentLength(buffer.readableByteCount());
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        setResponseStatus(exchange, httpStatus);
        return resp.writeWith(Flux.just(buffer));
    }

    /**
     * post,get等请求添加参数时源数据为空
     * @param exchange
     * @return
     */
    public static Mono<Void> writeNpeParamsResponse(ServerWebExchange exchange) {
        ResponseVo rpcResponse = ResponseVo.defaultFailureResponseVo();
        rpcResponse.setMessage("request add params attr is empty !");
        return WriteResponseSupport.write(exchange, rpcResponse, HttpStatus.BAD_GATEWAY);
    }

}
