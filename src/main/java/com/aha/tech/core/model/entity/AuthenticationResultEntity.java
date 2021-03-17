package com.aha.tech.core.model.entity;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @Author: luweihong
 * @Date: 2019/3/13
 *
 * 鉴权实体类
 */
public class AuthenticationResultEntity {

    /**
     * 授权接口调用结果
     */
//    private UserVo userVo;

    /**
     * basic 用户名
     */
    private String userName;

    /**
     * basic 密码
     */
    private String password;

    /**
     * 验证结果
     */
    private Integer code;

    /**
     * 辅助信息
     */
    private String message;

    private Boolean skipAuth;

    public AuthenticationResultEntity() {
        super();
    }

    public AuthenticationResultEntity(Boolean skipAuth, Integer code) {
        this.skipAuth = skipAuth;
        this.code = code;
    }


//    public UserVo getUserVo() {
//        return userVo;
//    }
//
//    public void setUserVo(UserVo userVo) {
//        this.userVo = userVo;
//    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getSkipAuth() {
        return skipAuth;
    }

    public void setSkipAuth(Boolean skipAuth) {
        this.skipAuth = skipAuth;
    }


    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}
