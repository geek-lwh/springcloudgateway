package com.aha.tech.core.filters;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.controller.resource.PassportResource;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import static com.aha.tech.commons.constants.ResponseConstants.SUCCESS;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/2/14
 */
@Component
public class AuthCheckGatewayFilterFactory implements GlobalFilter {

    private static final Logger logger = LoggerFactory.getLogger(GatewayFilter.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private PassportResource passportResource;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
        List<String> headersOfToken = requestHeaders.get("token");
        String accessToken = CollectionUtils.isEmpty(headersOfToken) ? StringUtils.EMPTY : headersOfToken.get(0);

        RpcResponse<UserVo> response = passportResource.verify(accessToken);
        int code = response.getCode();
        if (code != SUCCESS) {
            return Mono.defer(() -> {
                setResponseStatus(exchange, HttpStatus.UNAUTHORIZED);
                final ServerHttpResponse resp = exchange.getResponse();
                byte[] bytes = JSON.toJSONString(response).getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

                return resp.writeWith(Flux.just(buffer));
            });
        }
        UserVo userVo = response.getData();

        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String method = serverHttpRequest.getMethodValue();

//        if (method.equals(HttpMethod.POST)) {
        URI uri = serverHttpRequest.getURI();

        String resolveBody = resolveBodyFromRequest(serverHttpRequest);
        if (StringUtils.isBlank(resolveBody)) {
            logger.error("request body is empty");
            return Mono.defer(() -> {
                setResponseStatus(exchange, HttpStatus.BAD_REQUEST);
                final ServerHttpResponse resp = exchange.getResponse();
                RpcResponse responseBody = RpcResponse.defaultFailureResponse();
                responseBody.setMessage("request body is empty");

                byte[] bytes = new byte[0];
                try {
                    bytes = objectMapper.writeValueAsBytes(response);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

                return resp.writeWith(Flux.just(buffer));
            });
        }

        try {
            JSONObject obj = new JSONObject(resolveBody);
            obj.put("user_id", userVo.getUserId());
            resolveBody = obj.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

//        Map<String, Object> newBody = null;
//        try {
//            newBody = objectMapper.readValue(resolveBody, Map.class);
//            newBody.put("user_id", userVo.getUserId());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        DataBuffer bodyDataBuffer = stringBuffer(resolveBody);
        int len = bodyDataBuffer.readableByteCount();
        Flux<DataBuffer> bodyFlux = Flux.just(bodyDataBuffer);

        HttpHeaders myHeaders = new HttpHeaders();
        copyMultiValueMap(serverHttpRequest.getHeaders(), myHeaders);
        myHeaders.remove(HttpHeaders.CONTENT_LENGTH);
        myHeaders.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(len));

        ServerHttpRequest request = serverHttpRequest.mutate().uri(uri).build();
        request = new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return bodyFlux;
            }

            @Override
            public HttpHeaders getHeaders() {
                return myHeaders;
            }
        };

        return chain.filter(exchange.mutate().request(request).build());
//        }

    }

    /**
     * 从request对象中解析body,DataBuffer 转 String
     * @param serverHttpRequest
     * @return
     */
    private String resolveBodyFromRequest(ServerHttpRequest serverHttpRequest) {
        //获取请求体
//        Flux<DataBuffer> body = serverHttpRequest.getBody();
//
//        AtomicReference<String> bodyRef = new AtomicReference<>();
//        body.subscribe(buffer -> {
//            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer.asByteBuffer());
//            DataBufferUtils.release(buffer);
//            bodyRef.set(charBuffer.toString());
//        });
//
//        return bodyRef.get();

        Flux<DataBuffer> body = serverHttpRequest.getBody();
        StringBuilder sb = new StringBuilder();

        body.subscribe(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            String bodyString = new String(bytes, StandardCharsets.UTF_8);
            sb.append(bodyString);
        });
        String str = sb.toString();

        return str;
    }

    /**
     * 构建DataBuffer
     * @param value
     * @return
     */
    private DataBuffer stringBuffer(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
        DataBuffer buffer = nettyDataBufferFactory.allocateBuffer(bytes.length);
        buffer.write(bytes);
        return buffer;
    }

    private static <K, V> void copyMultiValueMap(MultiValueMap<K, V> source, MultiValueMap<K, V> target) {
        source.forEach((key, value) -> target.put(key, new LinkedList<>(value)));
    }

}
