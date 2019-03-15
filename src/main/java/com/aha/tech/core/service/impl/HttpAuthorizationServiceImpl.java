package com.aha.tech.core.service.impl;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.controller.resource.PassportResource;
import com.aha.tech.core.exception.EmptyBodyException;
import com.aha.tech.core.model.dto.Params;
import com.aha.tech.core.model.entity.AuthenticationEntity;
import com.aha.tech.core.service.AuthorizationService;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static com.aha.tech.commons.constants.ResponseConstants.SUCCESS;
import static com.aha.tech.core.constant.HeaderFieldConstant.DEFAULT_X_TOKEN_VALUE;
import static com.aha.tech.core.tools.BeanUtil.copyMultiValueMap;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 *
 */
@Service("httpAuthorizationService")
public class HttpAuthorizationServiceImpl implements AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(HttpAuthorizationServiceImpl.class);

    private static final char CHAR_AND_MARK = Separator.AND_MARK.toCharArray()[0];

    private static final String USER_ID_FIELD = "user_id";

    @Autowired(required = false)
    private PassportResource passportResource;

    /**
     * 校验访客信息的合法性
     * @param accessToken
     * @return
     */
    @Override
    public AuthenticationEntity verifyVisitor(String accessToken) {
        AuthenticationEntity authenticationEntity = new AuthenticationEntity();
        authenticationEntity.setVerifyResult(Boolean.TRUE);

        Boolean checkTokenValid = accessToken.equals(DEFAULT_X_TOKEN_VALUE);
        if (!checkTokenValid) {
            logger.warn("匿名用户令牌不合法, access token : {}", accessToken);
            authenticationEntity.setVerifyResult(Boolean.FALSE);
            return authenticationEntity;
        }

        // todo 白名单判断
//        if (!isWhite) {
//            logger.warn("不在维护的白名单列表内");
//            authenticationEntity.setVerifyResult(Boolean.FALSE);
//            return authenticationEntity;
//        }

        return authenticationEntity;
    }

    /**
     * 校验用户信息合法性
     * @param accessToken
     * @return
     */
    @Override
    public AuthenticationEntity verifyUser(String accessToken) {
        AuthenticationEntity authenticationEntity = new AuthenticationEntity();
        RpcResponse<UserVo> authResult = passportResource.verify(accessToken);
        int code = authResult.getCode();
        authenticationEntity.setVerifyResult(code == SUCCESS);
        authenticationEntity.setRpcResponse(authResult);

        return authenticationEntity;
    }

    /**
     * 根据httpMethod添加信息
     * @param serverHttpRequest
     * @param params
     * @return
     */
    public ServerHttpRequest overwriteParams(ServerHttpRequest serverHttpRequest, Params params) {
        HttpMethod httpMethod = serverHttpRequest.getMethod();
        if (params == null) {
            logger.error("没有找到正确的用户");
            return serverHttpRequest;
        }

        Long userId = params.getUserId();
        if (userId == null) {
            logger.error("没有找到正确的用户");
            return serverHttpRequest;
        }

        if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE) {
            return addQueryParams(serverHttpRequest, userId);
        }

        if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) {
            return modifyRequestBody(serverHttpRequest, userId);
        }

        logger.error("授权认证通过,但是没有进行参数添加,http method : {}", httpMethod);
        return serverHttpRequest;
    }

    /**
     * 构建新的请求体
     * @param serverHttpRequest
     * @return
     */
    private ServerHttpRequest addQueryParams(ServerHttpRequest serverHttpRequest, Long userId) {
        URI uri = serverHttpRequest.getURI();
        String originalQuery = serverHttpRequest.getURI().getRawQuery();

        StringBuilder query = new StringBuilder();
        if (StringUtils.hasText(originalQuery)) {
            query.append(originalQuery);
            if (originalQuery.charAt(originalQuery.length() - 1) != CHAR_AND_MARK) {
                query.append(Separator.AND_MARK);
            }
        }
        query.append("user_id").append("=").append(userId);

        URI newUri = UriComponentsBuilder.fromUri(uri)
                .replaceQuery(query.toString())
                .build(true)
                .toUri();

        return serverHttpRequest.mutate().uri(newUri).build();
    }

    /**
     * 构建新的请求
     * @param serverHttpRequest
     * @return
     */
    private ServerHttpRequest modifyRequestBody(ServerHttpRequest serverHttpRequest, Long userId) {
        String resolveBody = resolveBodyFromRequest(serverHttpRequest);
        if (org.apache.commons.lang3.StringUtils.isBlank(resolveBody)) {
            throw new EmptyBodyException();
        }

        ServerHttpRequest newRequest = addRequestBody(resolveBody, userId, serverHttpRequest);

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
     * @param userId
     * @param serverHttpRequest
     * @return
     */
    private ServerHttpRequest addRequestBody(String resolveBody, String userId, ServerHttpRequest serverHttpRequest) {
        JSONObject obj = JSON.parseObject(resolveBody);
        obj.put(USER_ID_FIELD, userId);
        DataBuffer bodyDataBuffer = stringBuffer(obj.toString());
        Flux<DataBuffer> bodyFlux = Flux.just(bodyDataBuffer);

        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();

        URI newUri = UriComponentsBuilder.fromUri(serverHttpRequest.getURI()).build(true).toUri();
        ServerHttpRequest newRequest = serverHttpRequest.mutate().uri(newUri).build();

        // 插入后计算新的content length,否则会出现异常
        HttpHeaders myHeaders = new HttpHeaders();
        copyMultiValueMap(httpHeaders, myHeaders);
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
