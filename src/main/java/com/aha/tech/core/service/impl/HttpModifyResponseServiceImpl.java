package com.aha.tech.core.service.impl;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.ModifyResponseService;
import com.aha.tech.core.support.AttributeSupport;
import com.aha.tech.util.LogUtil;
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
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.aha.tech.core.constant.HeaderFieldConstant.KEEP_ALIVE_VALUE;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
@Service("httpModifyResponseService")
public class HttpModifyResponseServiceImpl implements ModifyResponseService {

    private static final Logger logger = LoggerFactory.getLogger(HttpModifyResponseServiceImpl.class);

    /**
     * ???????????????
     * @param oldResponse
     * @return
     */
    @Deprecated
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
                LogUtil.combineTraceId(serverWebExchange);
                Mono modifiedBody = clientResponse.bodyToMono(String.class).flatMap(responseBody -> {
                    ResponseVo responseVo = ResponseVo.defaultFailureResponseVo();
                    try {
                        responseVo = JSON.parseObject(responseBody, ResponseVo.class);
                        AttributeSupport.putResponseLine(serverWebExchange, responseVo);

                    } catch (Exception e) {
                        logger.error("??????????????????????????? responseBody : {}", responseBody, e);
                    }
//                    String warnLog = ResponseSupport.buildWarnLog(serverWebExchange, responseVo, getDelegate().getStatusCode());
//                    if (StringUtils.isNotBlank(warnLog)) {
//                        logger.warn("???????????????! {}", warnLog);
//                    }
                    return Mono.just(responseBody);
                });

//                HttpHeaders requestHeader = serverWebExchange.getRequest().getHeaders();
                BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
                CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(serverWebExchange, oldResponse.getHeaders());
                return bodyInserter.insert(outputMessage, new BodyInserterContext())
                        .then(Mono.defer(() -> {
                            Flux<DataBuffer> messageBody = outputMessage.getBody();
                            HttpHeaders responseHeader = getDelegate().getHeaders();
                            responseHeader.remove(HttpHeaders.TRANSFER_ENCODING);
//                            modifyResponseHeader(responseHeader, requestHeader);
                            messageBody = messageBody.doOnNext(data -> responseHeader.setContentLength(data.readableByteCount()));
                            return getDelegate().writeWith(messageBody);
                        }));
            }
        };

        return serverHttpResponseDecorator;
    }

    /**
     * ??????????????????
     * httpHeaders.set ????????????
     *
     * @param responseHeader
     * @param requestHeader
     */
    @Deprecated
    public void modifyResponseHeader(HttpHeaders responseHeader, HttpHeaders requestHeader) {
//        responseHeader.setAccessControlAllowOrigin(HEADER_ALL_CONTROL_ALLOW_ORIGIN_ACCESS);
//        responseHeader.setAccessControlAllowMethods(HEADER_CROSS_ACCESS_ALLOW_HTTP_METHODS);
//        responseHeader.setAccessControlMaxAge(HEADER_CROSS_ACCESS_ALLOW_MAX_AGE);
//        responseHeader.setAccessControlAllowHeaders(HEADER_CROSS_ACCESS_ALLOW_ALLOW_HEADERS);
        String keepAlive = requestHeader.getFirst(HeaderFieldConstant.HEADER_CONNECTION);
        responseHeader.set(HeaderFieldConstant.HEADER_CONNECTION, StringUtils.isBlank(keepAlive) ? KEEP_ALIVE_VALUE : keepAlive);
    }

    /**
     * ?????????gzip??????
     * @param serverHttpRequest
     * @return
     */
    @Deprecated
    private boolean isGZipped(ServerHttpRequest serverHttpRequest) {
        String requestEncoding = serverHttpRequest.getHeaders().getFirst(HttpHeaders.ACCEPT_ENCODING);
        if (requestEncoding.indexOf("gzip") == -1) {
            return false;
        }
        return true;
    }

}
