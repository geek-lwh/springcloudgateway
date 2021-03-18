package com.aha.tech.core.support;

import com.aha.tech.commons.constants.ResponseConstants;
import com.aha.tech.core.model.entity.SnapshotRequestEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.util.LogUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/3/25
 *
 * 返回response信息辅助类
 */
public class ResponseSupport {

    private static final Logger logger = LoggerFactory.getLogger(ResponseSupport.class);

    /**
     * 写入response body
     * @param exchange
     * @param responseVo
     * @param httpStatus
     * @return
     */
    public static Mono<Void> interrupt(ServerWebExchange exchange, ResponseVo responseVo, HttpStatus httpStatus, Exception e) {
        DataBuffer buffer = constructResponse(exchange, responseVo, httpStatus);
        LogUtil.splicingError(exchange, e);
        return exchange.getResponse().writeWith(Flux.just(buffer));
    }

    /**
     * 写入response body
     * @param exchange
     * @param httpStatus
     * @param responseVo
     * @return
     */
    public static Mono<Void> interrupt(ServerWebExchange exchange, HttpStatus httpStatus, ResponseVo responseVo) {
        logger.warn(responseVo.getMessage());
        DataBuffer buffer = constructResponse(exchange, responseVo, httpStatus);

        return exchange.getResponse().writeWith(Flux.just(buffer));
    }

    /**
     * 构建response返回体
     * @param exchange
     * @param responseVo
     * @param httpStatus
     * @return
     */
    public static DataBuffer constructResponse(ServerWebExchange exchange, ResponseVo responseVo, HttpStatus httpStatus) {
        final ServerHttpResponse resp = exchange.getResponse();
        byte[] bytes = JSON.toJSONString(responseVo).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
        resp.getHeaders().setContentLength(buffer.readableByteCount());
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        Boolean isOldVersion = AttributeSupport.isOldVersion(exchange);
        if (isOldVersion) {
            setResponseStatus(exchange, HttpStatus.OK);
        } else {
            setResponseStatus(exchange, httpStatus);
        }

        return buffer;
    }

    /**
     * 构建日志
     * @param exchange
     * @param responseVo
     * @param httpStatus
     * @return
     */
    @Deprecated
    public static String buildWarnLog(ServerWebExchange exchange, ResponseVo responseVo, HttpStatus httpStatus) {
        SnapshotRequestEntity snapshotRequestEntity = AttributeSupport.getSnapshotRequest(exchange);
//        String requestId = ExchangeSupport.getTraceId(exchange);

        URI uri = snapshotRequestEntity.getRequestLine();
        if (uri == null) {
            snapshotRequestEntity.setRequestLine(exchange.getRequest().getURI());
        }

        HttpHeaders httpHeaders = snapshotRequestEntity.getAfterModifyRequestHttpHeaders();
        if (httpHeaders == null) {
            snapshotRequestEntity.setAfterModifyRequestHttpHeaders(exchange.getRequest().getHeaders());
        }

        Integer code = responseVo.getCode();
        if (!code.equals(ResponseConstants.SUCCESS) || !httpStatus.equals(HttpStatus.OK)) {
            StringBuffer sb = new StringBuffer();
//            sb.append("requestId : ").append(requestId).append(System.lineSeparator());
            sb.append("response body: ").append(responseVo).append(System.lineSeparator());
            sb.append("http status : ").append(httpStatus).append(System.lineSeparator());
            sb.append("info : ").append(snapshotRequestEntity).append(System.lineSeparator());
            return sb.toString();
        }

        return org.apache.commons.lang3.StringUtils.EMPTY;
    }

}
