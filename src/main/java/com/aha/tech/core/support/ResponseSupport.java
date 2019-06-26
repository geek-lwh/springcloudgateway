package com.aha.tech.core.support;

import com.aha.tech.commons.constants.ResponseConstants;
import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.AccessLogService;
import com.aha.tech.util.SpringContextUtil;
import com.alibaba.fastjson.JSON;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
     * @return
     */
    public static Mono<Void> write(ServerWebExchange exchange, ResponseVo responseVo, Exception e) {
        final ServerHttpResponse resp = exchange.getResponse();
        byte[] bytes = JSON.toJSONString(responseVo).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
        resp.getHeaders().setContentLength(buffer.readableByteCount());
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        setResponseStatus(exchange, HttpStatus.OK);
        AccessLogService httpAccessLogService = (AccessLogService) SpringContextUtil.getBean("httpAccessLogService");
        httpAccessLogService.printWhenError(exchange, e);
        return resp.writeWith(Flux.just(buffer));
    }

    /**
     * 写入response body
     * @param exchange
     * @param responseVo
     * @return
     */
    public static Mono<Void> write(ServerWebExchange exchange, ResponseVo responseVo) {
        logger.warn(responseVo.getMessage());
        final ServerHttpResponse resp = exchange.getResponse();
        byte[] bytes = JSON.toJSONString(responseVo).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = resp.bufferFactory().wrap(bytes);
        resp.getHeaders().setContentLength(buffer.readableByteCount());
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        setResponseStatus(exchange, HttpStatus.OK);
        return resp.writeWith(Flux.just(buffer));
    }

    /**
     * 构建劲爆日志
     * @param requestId
     * @param cacheRequestEntity
     * @param responseVo
     * @param httpStatus
     * @return
     */
    public static String buildWarnLog(String requestId, CacheRequestEntity cacheRequestEntity, ResponseVo responseVo, HttpStatus httpStatus) {
        Integer code = responseVo.getCode();
        if (!code.equals(ResponseConstants.SUCCESS) || !httpStatus.equals(HttpStatus.OK)) {
            StringBuffer sb = new StringBuffer();
            sb.append(System.lineSeparator());
            sb.append("requestId : ").append(requestId).append(System.lineSeparator());
            sb.append("response body: ").append(responseVo).append(System.lineSeparator());
            sb.append("http status : ").append(httpStatus).append(System.lineSeparator());
            sb.append("info : ").append(cacheRequestEntity).append(System.lineSeparator());
            return sb.toString();
        }

        return org.apache.commons.lang3.StringUtils.EMPTY;
    }

    /**
     * 格式化输出httpheaders
     * @param httpHeaders
     * @return
     */
    public static String formatHttpHeaders(HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return Strings.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        httpHeaders.forEach((String k, List<String> v) -> sb.append(k).append(Separator.COLON_MARK).append(StringUtils.collectionToDelimitedString(v, Separator.COMMA_MARK)).append(System.lineSeparator()));

        return sb.toString();
    }

}
