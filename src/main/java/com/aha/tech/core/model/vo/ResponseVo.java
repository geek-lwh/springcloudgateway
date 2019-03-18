package com.aha.tech.core.model.vo;

/**
 * @Author: luweihong
 * @Date: 2019/2/28
 */
public class ResponseVo<T> {

    private int code;

    private String message;

    private String cursor;

    private T data;

    public ResponseVo(){
        super();
    }

    public ResponseVo(int code, String message, String cursor, T data) {
        this.code = code;
        this.message = message;
        this.cursor = cursor;
        this.data = data;
    }

    public static ResponseVo defaultFailureResponseVo(){
        ResponseVo responseVo = new ResponseVo();
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
}
