package com.aha.tech.core.model.vo;

import com.aha.tech.commons.constants.ResponseConstants;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.http.HttpStatus;

/**
 * @Author: luweihong
 * @Date: 2019/2/28
 */
public class ResponseVo<T> {

    private int code;

    private String message;

    private String cursor;

    private T data;

    public ResponseVo() {
        super();
    }

    public ResponseVo(int code) {
        this.code = code;
    }

    public ResponseVo(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ResponseVo(int code, String message, String cursor, T data) {
        this.code = code;
        this.message = message;
        this.cursor = cursor;
        this.data = data;
    }

    public static ResponseVo defaultFailureResponseVo() {
        return new ResponseVo(ResponseConstants.FAILURE);
    }

    /**
     * 构建一个校验请求失败的返回体
     * @return
     */
    public static ResponseVo getFailEncryptResponseVo() {
        ResponseVo responseVo = new ResponseVo();
        responseVo.setCode(HttpStatus.FORBIDDEN.value());
        responseVo.setMessage("");

        return responseVo;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}
