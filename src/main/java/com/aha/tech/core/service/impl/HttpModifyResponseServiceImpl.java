package com.aha.tech.core.service.impl;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.commons.response.RpcResponsePage;
import com.aha.tech.core.exception.DecryptResponseBodyException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.ModifyResponseService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static com.aha.tech.core.constant.HeaderFieldConstant.*;
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
                logger.debug("开始修改返回值过滤器");

                ModifyResponseBodyGatewayFilterFactory m = new ModifyResponseBodyGatewayFilterFactory(ServerCodecConfigurer.create());
                String originalResponseContentType = serverWebExchange.getAttribute(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.add(HttpHeaders.CONTENT_TYPE, originalResponseContentType);
                ResponseAdapter responseAdapter = m.new ResponseAdapter(body, httpHeaders);
                DefaultClientResponse clientResponse = new DefaultClientResponse(responseAdapter, ExchangeStrategies.withDefaults());

                Mono modifiedBody = clientResponse.bodyToMono(String.class).flatMap(originalBody -> {
                    logger.info("response body : {}", originalBody);
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
