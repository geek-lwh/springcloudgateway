package com.aha.tech.core.service.impl;

import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.ModifyResponseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.aha.tech.core.constant.HeaderFieldConstant.*;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
@Service("httpModifyResponseService")
public class HttpModifyResponseServiceImpl implements ModifyResponseService {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 修改返回体
     * @param serverHttpResponse
     * @return
     */
    @Override
    public ServerHttpResponseDecorator modifyBody(ServerHttpResponse serverHttpResponse) {
        DataBufferFactory bufferFactory = serverHttpResponse.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(serverHttpResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                Flux<? extends DataBuffer> flux = (Flux<? extends DataBuffer>) body;
                if (body instanceof Flux) {
                    return super.writeWith(
                            flux.buffer().map(dataBuffers -> {
                                ByteOutputStream outputStream = new ByteOutputStream();
                                dataBuffers.forEach(i -> {
                                    byte[] array = new byte[i.readableByteCount()];
                                    i.read(array);
                                    outputStream.write(array);
                                });

                                byte[] stream = outputStream.getBytes();
                                try {
                                    DataBuffer d = decryptBody(stream, bufferFactory);
                                    if(d != null){
                                        serverHttpResponse.getHeaders().setContentLength(d.readableByteCount());
                                        return d;
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                return bufferFactory.wrap(stream);
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
    private DataBuffer decryptBody(byte[] stream, DataBufferFactory dataBufferFactory) throws IOException {
        ResponseVo responseVo = objectMapper.readValue(stream, ResponseVo.class);
        String cursor = responseVo.getCursor();
        if (StringUtils.isNotBlank(cursor)) {
            byte[] decodeCursor = Base64.decodeBase64(cursor);
            responseVo.setCursor(new String(decodeCursor, StandardCharsets.UTF_8));
            return dataBufferFactory.wrap(objectMapper.writeValueAsBytes(responseVo));
        }

        return null;
    }
}
