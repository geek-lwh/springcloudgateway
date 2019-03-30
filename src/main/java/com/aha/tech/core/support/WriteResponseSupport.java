package com.aha.tech.core.support;

import com.aha.tech.core.exception.GatewayException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.alibaba.fastjson.JSON;
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

    /**
     * 根据异常值返回
     * @param exchange
     * @param gatewayException
     * @return
     */
    public static Mono<Void> writeError(ServerWebExchange exchange, GatewayException gatewayException){
        final ServerHttpResponse resp = exchange.getResponse();
        ResponseVo responseVo = new ResponseVo(gatewayException.getCode(),gatewayException.getMessage());
        byte[] bytes = JSON.toJSONString(responseVo).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
        resp.getHeaders().setContentLength(buffer.readableByteCount());
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        setResponseStatus(exchange,HttpStatus.BAD_GATEWAY);
        return resp.writeWith(Flux.just(buffer));
    }

    /**
     * 写入response body
     * @param exchange
     * @param responseVo
     * @param httpStatus
     * @return
     */
    public static Mono<Void> write(ServerWebExchange exchange, ResponseVo responseVo,HttpStatus httpStatus){
        final ServerHttpResponse resp = exchange.getResponse();
        byte[] bytes = JSON.toJSONString(responseVo).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
        resp.getHeaders().setContentLength(buffer.readableByteCount());
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        setResponseStatus(exchange,httpStatus);
        return resp.writeWith(Flux.just(buffer));
    }

    /**
     * post,get等请求添加参数时源数据为空
     * @param exchange
     * @return
     */
    public static Mono<Void> writeNpeParamsResponse(ServerWebExchange exchange){
        ResponseVo rpcResponse = ResponseVo.defaultFailureResponseVo();
        rpcResponse.setMessage("request add params attr is empty !");
        return WriteResponseSupport.write(exchange, rpcResponse, HttpStatus.BAD_GATEWAY);
    }


}
