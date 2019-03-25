package com.aha.tech.core.service.impl;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.commons.response.RpcResponsePage;
import com.aha.tech.core.exception.DecryptResponseBodyException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.ModifyResponseService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static com.aha.tech.core.constant.HeaderFieldConstant.*;

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
     * @param serverHttpResponse
     * @return
     */
    @Override
    public ServerHttpResponseDecorator modifyBody(ServerWebExchange serverWebExchange, ServerHttpResponse serverHttpResponse) {
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
                                DataBuffer data = decryptBody(stream, bufferFactory);
                                serverHttpResponse.getHeaders().setContentLength(data.readableByteCount());

                                return data;
                            }));
                }

                return super.writeWith(body);
            }

        };

        return decoratedResponse;
    }

    /**
     * 修改返回体的报头信息
     * @param httpHeaders
     */
    @Override
    public void modifyHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        // 表明服务器允许客户端使用 POST,PUT,GET,DELETE 发起请求
        httpHeaders.add(HEADER_ACCESS_CONTROL_ALLOW_METHODS, "POST,PUT,GET,DELETE");

        // 表明该响应的有效时间为 10 秒
        httpHeaders.add(HEADER_ACCESS_CONTROL_MAX_AGE, "10");

        // 表明服务器允许请求中携带字段 X-PINGOTHER 与 Content-Type x-requested-with
        httpHeaders.add(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, "x-requested-with,Content-Type");
    }

    /**
     * 对response body 进行解码
     * @param stream
     * @return
     */
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
