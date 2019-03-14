package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.vo.ResponseVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.aha.tech.core.constant.FilterOrderedConstant.GLOBAL_MODIFY_RESPONSE_BODY_GATEWAY_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 * 修改response body 网关过滤器
 */
@Component
public class ModifyResponseBodyGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyResponseBodyGatewayFilter.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return GLOBAL_MODIFY_RESPONSE_BODY_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行修改response body 网关过滤器");
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpResponseDecorator newResponse = modifyResponse(response);

        ServerWebExchange swe = exchange.mutate().response(newResponse).build();

        return chain.filter(swe);
    }

    /**
     * 获取response并且修改
     * @param response
     * @return
     */
    private ServerHttpResponseDecorator modifyResponse(ServerHttpResponse response) {
        DataBufferFactory bufferFactory = response.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
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
                                        response.getHeaders().setContentLength(d.readableByteCount());
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
