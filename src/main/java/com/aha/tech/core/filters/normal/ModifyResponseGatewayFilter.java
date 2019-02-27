package com.aha.tech.core.filters.normal;

import com.aha.tech.commons.response.RpcResponsePage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
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

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 * 修改http response 返回值
 */
@Component
public class ModifyResponseGatewayFilter implements GatewayFilter {

    private static final Logger logger = LoggerFactory.getLogger(ModifyResponseGatewayFilter.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpResponseDecorator serverHttpResponseDecorator = modifyResponse(response);

        ServerWebExchange swe = exchange.mutate().response(serverHttpResponseDecorator).build();
        return chain.filter(swe);
    }

    /**
     * 获取response并且修改
     * @param response
     * @return
     */
    private ServerHttpResponseDecorator modifyResponse(ServerHttpResponse response){
        DataBufferFactory dataBufferFactory = response.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                Flux<? extends DataBuffer> flux = (Flux<? extends DataBuffer>) body;
                Flux<? extends DataBuffer> f = flux.flatMap(dataBuffer -> {
                    byte[] origRespContent = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(origRespContent);
                    logger.debug("content : {}", new String(origRespContent));
                    DataBuffer responseBody = null;
                    try {
                        RpcResponsePage rpcResponsePage = objectMapper.readValue(origRespContent, RpcResponsePage.class);
                        String cursor = rpcResponsePage.getCursor();
                        if (StringUtils.isNotBlank(cursor)) {
                            byte[] decodeCursor = Base64.decodeBase64(cursor);
                            rpcResponsePage.setCursor(new String(decodeCursor, StandardCharsets.UTF_8));
                        }
                        byte[] data = objectMapper.writeValueAsBytes(rpcResponsePage);
                        responseBody = dataBufferFactory.allocateBuffer(data.length);
                        responseBody.write(data);
                    } catch (IOException e) {
                        logger.error(e.getMessage(),e);
                    }

                    return Flux.just(responseBody);
                });

                return super.writeWith(f);
            }
        };

        return decoratedResponse;
    }
}
