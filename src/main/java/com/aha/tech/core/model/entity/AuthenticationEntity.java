package com.aha.tech.core.model.entity;

import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @Author: luweihong
 * @Date: 2019/3/13
 *
 * 鉴权实体类
 */
public class AuthenticationEntity {

    /**
     * 授权接口调用结果
     */
    private UserVo userVo;

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
    private Boolean verifyResult;

    public AuthenticationEntity() {
        super();
    }

    public UserVo getUserVo() {
        return userVo;
    }

    public void setUserVo(UserVo userVo) {
        this.userVo = userVo;
    }

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

    public Boolean getVerifyResult() {
        return verifyResult;
    }

    public void setVerifyResult(Boolean verifyResult) {
        this.verifyResult = verifyResult;
    }

    @Override
    public String toString(){
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}
