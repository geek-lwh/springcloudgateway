package com.aha.tech.core.handler;

import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.exception.GatewayException;
import com.google.common.collect.Maps;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;

/**
 *  统一异常处理
 */
public class JsonExceptionHandler extends DefaultErrorWebExceptionHandler {

    private static final int NO_MATCHING_HANDLER_ERROR_CODE = 404;

    private static final String NO_MATCHING_HANDLER_ERROR_MSG = "无效的资源路径";

    /**
     * Create a new {@code DefaultErrorWebExceptionHandler} instance.
     * @param errorAttributes the error attributes
     * @param resourceProperties the resources configuration properties
     * @param errorProperties the error configuration properties
     * @param applicationContext the current application context
     */
    public JsonExceptionHandler(ErrorAttributes errorAttributes, ResourceProperties resourceProperties, ErrorProperties errorProperties, ApplicationContext applicationContext) {
        super(errorAttributes, resourceProperties, errorProperties, applicationContext);
    }

    /**
     * 获取异常属性
     */
    @Override
    protected Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
        int code = 500;
        String message = "";
        Throwable error = super.getError(request);

        if (error instanceof NotFoundException || error instanceof ResponseStatusException) {
            code = NO_MATCHING_HANDLER_ERROR_CODE;
            message = NO_MATCHING_HANDLER_ERROR_MSG;
        }

        if (error instanceof GatewayException) {
            GatewayException e = (GatewayException) error;
            code = e.getCode();
            message = e.getMessage();
        }

        String method = request.methodName();
        URI uri = request.uri();
        String requestUrl = this.buildRequestUrl(method, uri);

        return buildResponseData(error, code, requestUrl, message);
    }

    /**
     * 指定响应处理方法为JSON处理的方法
     * @param errorAttributes
     */
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    /**
     * 根据code获取对应的HttpStatus
     * @param errorAttributes
     */
    @Override
    protected HttpStatus getHttpStatus(Map<String, Object> errorAttributes) {
        int statusCode = (int) errorAttributes.get("code");
        HttpStatus httpStatus;
        try {
            httpStatus = HttpStatus.valueOf(statusCode);
        } catch (Exception e) {
            httpStatus = HttpStatus.valueOf(500);
        }
        return httpStatus;
    }

    /**
     * 构建异常信息
     * @param methodName
     * @param uri
     * @return
     */
    private String buildRequestUrl(String methodName, URI uri) {
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append(methodName);
        errorMsg.append(" ");
        errorMsg.append(uri);
        return errorMsg.toString();
    }

    /**
     * 构建返回的JSON数据格式
     * @param status        状态码
     * @param requestUrl  请求路径
     * @param message  异常信息
     * @return
     */
    public static Map<String, Object> buildResponseData(Throwable ex, int status, String requestUrl, String message) {
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(5);
        map.put("code", status);
        map.put("url", requestUrl);
        map.put("message", message);
        map.put("date", DateUtil.currentDateByDefaultFormat());
        map.put("trace", ex.getStackTrace()[0]);

        return map;
    }

}
