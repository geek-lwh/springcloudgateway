package com.aha.tech.core.filters.global;

import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.constant.FilterOrderedConstant;
import com.aha.tech.core.exception.EmptyBodyException;
import com.aha.tech.core.exception.GatewayException;
import com.aha.tech.core.handler.SessionHandler;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static com.aha.tech.core.constant.GatewayAttributeConstant.SKIP_AUTHORIZATION;
import static com.aha.tech.core.tools.BeanUtil.copyMultiValueMap;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 *
 * 调用授权系统解析用户主键
 * 修改body请求体
 */
@Component
public class AddRequestBodyGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AddRequestBodyGatewayFilter.class);

    @Override
    public int getOrder() {
        return FilterOrderedConstant.GLOBAL_ADD_REQUEST_BODY_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行添加post参数过滤器");
        Boolean skipAuthorization = (Boolean) exchange.getAttributes().get(SKIP_AUTHORIZATION);

        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        HttpMethod httpMethod = serverHttpRequest.getMethod();
        URI uri = serverHttpRequest.getURI();

        if (skipAuthorization.equals(Boolean.TRUE) || httpMethod != HttpMethod.POST) {
            logger.info("请求路径: {} 跳过过授权", uri.getRawPath());
            return chain.filter(exchange);
        }

        ServerHttpRequest newRequest = modifyRequestBodyEntity(serverHttpRequest);
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    /**
     * 构建新的请求
     * @param serverHttpRequest
     * @return
     */
    private ServerHttpRequest modifyRequestBodyEntity(ServerHttpRequest serverHttpRequest) {
        String resolveBody = resolveBodyFromRequest(serverHttpRequest);
        if (StringUtils.isBlank(resolveBody)) {
            throw new EmptyBodyException();
        }

        UserVo userVo = SessionHandler.get();
        URI newUri = UriComponentsBuilder.fromUri(serverHttpRequest.getURI()).build(true).toUri();
        ServerHttpRequest newRequest;
        try {
            newRequest = addRequestBody(resolveBody, userVo, serverHttpRequest, serverHttpRequest.mutate().uri(newUri).build());
        } catch (Exception e) {
            logger.error("date : {} mutate new request has error", DateUtil.currentDateByDefaultFormat(), e);
            throw new GatewayException(e);
        }finally {
            SessionHandler.remove();
        }

        return newRequest;
    }

    /**
     * 从request对象中解析body,DataBuffer 转 String
     * @param serverHttpRequest
     * @return
     */
    private String resolveBodyFromRequest(ServerHttpRequest serverHttpRequest) {
        Flux<DataBuffer> body = serverHttpRequest.getBody();

        AtomicReference<String> bodyRef = new AtomicReference<>();
        body.subscribe(buffer -> {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer.asByteBuffer());
            DataBufferUtils.release(buffer);
            bodyRef.set(charBuffer.toString());
        });

        return bodyRef.get();
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


    /**
     * 添加request body
     * @param resolveBody
     * @param userVo
     * @param serverHttpRequest
     * @param newRequest
     * @return
     * @throws JSONException
     */
    private ServerHttpRequest addRequestBody(String resolveBody, UserVo userVo, ServerHttpRequest serverHttpRequest, ServerHttpRequest newRequest) throws JSONException {
        JSONObject obj = JSON.parseObject(resolveBody);
        obj.put("user_id", userVo.getUserId());
        DataBuffer bodyDataBuffer = stringBuffer(obj.toString());
        Flux<DataBuffer> bodyFlux = Flux.just(bodyDataBuffer);

        // 插入后计算新的content length,否则会出现异常
        HttpHeaders myHeaders = new HttpHeaders();
        copyMultiValueMap(serverHttpRequest.getHeaders(), myHeaders);
        myHeaders.remove(HttpHeaders.CONTENT_LENGTH);
        int len = bodyDataBuffer.readableByteCount();
        myHeaders.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(len));
        newRequest = new ServerHttpRequestDecorator(newRequest) {
            @Override
            public Flux<DataBuffer> getBody() {
                return bodyFlux;
            }

            @Override
            public HttpHeaders getHeaders() {
                return myHeaders;
            }
        };

        return newRequest;
    }
}
