package com.aha.tech.core.service.impl;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.commons.response.RpcResponsePage;
import com.aha.tech.core.exception.DecryptResponseBodyException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.ModifyResponseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory.ResponseAdapter;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.DefaultClientResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
@Service("httpModifyResponseService")
public class HttpModifyResponseServiceImpl implements ModifyResponseService {

    private static final Logger logger = LoggerFactory.getLogger(HttpModifyResponseServiceImpl.class);

    @Autowired
    private ObjectMapper objectMapper;

    private final static String ALL_CONTROL_ALLOW_ORIGIN_ACCESS = "*";

    private final static long CROSS_ACCESS_ALLOW_MAX_AGE = 30l;

    private final static List<HttpMethod> CROSS_ACCESS_ALLOW_HTTP_METHODS = Lists.newArrayList(HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PUT);

    private final static List<String> CROSS_ACCESS_ALLOW_ALLOW_HEADERS = Lists.newArrayList("Authorization", "Origin", "X-Requested-With", "X-Env", "X-Request-Page", "Content-Type", "Accept");

    /**
     * 修改返回体  todo 看看能不能改成新版的
     * @param serverHttpResponse
     * @return
     */
    @Override
    public ServerHttpResponseDecorator modifyBodyAndHeaders(ServerWebExchange serverWebExchange, ServerHttpResponse serverHttpResponse) {
        ServerHttpResponse oldResponse = serverWebExchange.getResponse();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(oldResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                ModifyResponseBodyGatewayFilterFactory m = new ModifyResponseBodyGatewayFilterFactory(ServerCodecConfigurer.create());
                String originalResponseContentType = serverWebExchange.getAttribute(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.add(HttpHeaders.CONTENT_TYPE, originalResponseContentType);
                ResponseAdapter responseAdapter = m.new ResponseAdapter(body, httpHeaders);
                DefaultClientResponse clientResponse = new DefaultClientResponse(responseAdapter, ExchangeStrategies.withDefaults());

                Mono modifiedBody = clientResponse.bodyToMono(String.class).flatMap(originalBody -> Mono.just(originalBody));

                BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
                CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(serverWebExchange, serverWebExchange.getResponse().getHeaders());
                return bodyInserter.insert(outputMessage, new BodyInserterContext())
                        .then(Mono.defer(() -> {
                            Flux<DataBuffer> messageBody = outputMessage.getBody();
                            HttpHeaders headers = getDelegate().getHeaders();
                            crossAccessSetting(headers);
                            if (!headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {
                                messageBody = messageBody.doOnNext(data -> {
                                    headers.setContentLength(data.readableByteCount());
                                });
                            }
                            return getDelegate().writeWith(messageBody);
                        }));
            }
        };

        return decoratedResponse;
    }

    /**
     * 跨域访问设置
     * @param httpHeaders
     */
    @Override
    public void crossAccessSetting(HttpHeaders httpHeaders) {
        httpHeaders.setAccessControlAllowOrigin(ALL_CONTROL_ALLOW_ORIGIN_ACCESS);
        httpHeaders.setAccessControlAllowMethods(CROSS_ACCESS_ALLOW_HTTP_METHODS);
        httpHeaders.setAccessControlMaxAge(CROSS_ACCESS_ALLOW_MAX_AGE);
        httpHeaders.setAccessControlAllowHeaders(CROSS_ACCESS_ALLOW_ALLOW_HEADERS);
    }

    /**
     * 对response body 进行解码
     * 暂时不对response body做修改
     * @param stream
     * @return
     */
    @Deprecated
    private DataBuffer decryptBody(byte[] stream, DataBufferFactory dataBufferFactory) {
        try {
            ResponseVo responseVo = objectMapper.readValue(stream, ResponseVo.class);
            int code = responseVo.getCode();
            String message = responseVo.getMessage();
            Object data = responseVo.getData();
            String cursor = responseVo.getCursor();
            if (StringUtils.isNotBlank(cursor)) {
                byte[] decodeCursor = Base64.decodeBase64(cursor);
                String decryptCursor = new String(decodeCursor, StandardCharsets.UTF_8);
                RpcResponsePage rpcResponsePage = new RpcResponsePage(decryptCursor, code, message, data);
                logger.debug("返回值: {}", rpcResponsePage);

                return dataBufferFactory.wrap(objectMapper.writeValueAsBytes(rpcResponsePage));
            }

            RpcResponse rpcResponse = new RpcResponse(code, message, data);
            logger.debug("返回值: {}", rpcResponse);

            return dataBufferFactory.wrap(objectMapper.writeValueAsBytes(rpcResponse));
        } catch (Exception e) {
            throw new DecryptResponseBodyException();
        }
    }
}
