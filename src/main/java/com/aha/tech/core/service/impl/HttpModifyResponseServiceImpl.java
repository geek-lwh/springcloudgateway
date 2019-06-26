package com.aha.tech.core.service.impl;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.ModifyResponseService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory.ResponseAdapter;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.DefaultClientResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

import static com.aha.tech.core.constant.HeaderFieldConstant.*;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
@Service("httpModifyResponseService")
public class HttpModifyResponseServiceImpl implements ModifyResponseService {

    private static final Logger logger = LoggerFactory.getLogger(HttpModifyResponseServiceImpl.class);

    @Resource
    private ThreadPoolTaskExecutor writeLoggingThreadPool;

    /**
     * 修改返回体
     * @param oldResponse
     * @return
     */
    @Override
    public ServerHttpResponseDecorator renewResponse(ServerWebExchange serverWebExchange, ServerHttpResponse oldResponse) {
        ServerHttpResponseDecorator serverHttpResponseDecorator = new ServerHttpResponseDecorator(oldResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {

                ModifyResponseBodyGatewayFilterFactory m = new ModifyResponseBodyGatewayFilterFactory(ServerCodecConfigurer.create());
                String originalResponseContentType = serverWebExchange.getAttributeOrDefault(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR, MediaType.APPLICATION_JSON_UTF8_VALUE);
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.add(HttpHeaders.CONTENT_TYPE, originalResponseContentType);
                ResponseAdapter responseAdapter = m.new ResponseAdapter(body, httpHeaders);
                DefaultClientResponse clientResponse = new DefaultClientResponse(responseAdapter, ExchangeStrategies.withDefaults());
                Mono modifiedBody = clientResponse.bodyToMono(String.class).flatMap(originalBody -> {
                    CompletableFuture.runAsync(() -> {
                        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(serverWebExchange);
                        String requestId = ExchangeSupport.getRequestId(serverWebExchange);
                        ResponseVo responseVo = JSON.parseObject(originalBody, ResponseVo.class);
                        HttpStatus httpStatus = getDelegate().getStatusCode();
                        String warnLog = ResponseSupport.buildWarnLog(requestId, cacheRequestEntity, responseVo, httpStatus);
                        if (StringUtils.isNotBlank(warnLog)) {
                            logger.warn("状态码异常 =======> {}", warnLog);
                        }
                    }, writeLoggingThreadPool);

                    return Mono.just(originalBody);
                });

                BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
                CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(serverWebExchange, oldResponse.getHeaders());
                return bodyInserter.insert(outputMessage, new BodyInserterContext())
                        .then(Mono.defer(() -> {
                            Flux<DataBuffer> messageBody = outputMessage.getBody();
                            HttpHeaders headers = getDelegate().getHeaders();
                            headers.remove(HttpHeaders.TRANSFER_ENCODING);
                            crossAccessSetting(headers);
                            messageBody = messageBody.doOnNext(data -> headers.setContentLength(data.readableByteCount()));
//                            if (!headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {
//                                messageBody = messageBody.doOnNext(data -> headers.setContentLength(data.readableByteCount()));
//                            }
                            return getDelegate().writeWith(messageBody);
                        }));
            }
        };

        return serverHttpResponseDecorator;
    }

    /**
     * 跨域访问设置
     * httpHeaders.set 有则替换
     *
     * @param httpHeaders
     */
    @Override
    public void crossAccessSetting(HttpHeaders httpHeaders) {
        // crossDomain 设置了一次*,后端rs 又设置了一次*,所以需要在response的时候进行先删除,再设置
//        httpHeaders.remove(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN);
        httpHeaders.setAccessControlAllowOrigin(HEADER_ALL_CONTROL_ALLOW_ORIGIN_ACCESS);
        httpHeaders.setAccessControlAllowMethods(HEADER_CROSS_ACCESS_ALLOW_HTTP_METHODS);
        httpHeaders.setAccessControlMaxAge(HEADER_CROSS_ACCESS_ALLOW_MAX_AGE);
        httpHeaders.setAccessControlAllowHeaders(HEADER_CROSS_ACCESS_ALLOW_ALLOW_HEADERS);
    }

    /**
     * 是否是gzip压缩
     * @param serverHttpRequest
     * @return
     */
    private boolean isGZipped(ServerHttpRequest serverHttpRequest) {
        String requestEncoding = serverHttpRequest.getHeaders().getFirst(HttpHeaders.ACCEPT_ENCODING);
        if (requestEncoding.indexOf("gzip") == -1) {
            return false;
        }
        return true;
    }

}
