package com.aha.tech.core.service.impl;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.commons.response.RpcResponsePage;
import com.aha.tech.core.exception.DecryptResponseBodyException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.ModifyResponseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
     * 修改返回体
     * @param serverHttpResponse
     * @return
     */
    @Override
    public ServerHttpResponseDecorator modifyBodyAndHeaders(ServerWebExchange serverWebExchange, ServerHttpResponse serverHttpResponse) {
        DataBufferFactory bufferFactory = serverHttpResponse.bufferFactory();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(serverHttpResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> flux = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(
                            flux.buffer().map(dataBuffers -> {
                                ByteOutputStream outputStream = new ByteOutputStream();
                                dataBuffers.forEach(i -> {
                                    byte[] array = new byte[i.readableByteCount()];
                                    i.read(array);
                                    outputStream.write(array);
                                });

                                byte[] stream = outputStream.getBytes();
//                                DataBuffer data = decryptBody(stream, bufferFactory);

                                // 设置response 的 content-length
                                HttpHeaders httpHeaders = serverHttpResponse.getHeaders();
                                crossAccessSetting(httpHeaders);
//                                long contentLength = data.readableByteCount();
//                                if (contentLength > 0) {
//                                    httpHeaders.setContentLength(contentLength);
//                                } else {
//                                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
//                                }

                                return bufferFactory.wrap(stream);
                            }));
                }

                return super.writeWith(body);
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
