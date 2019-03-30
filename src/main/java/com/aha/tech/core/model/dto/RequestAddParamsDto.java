package com.aha.tech.core.model.dto;


import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @Author: monkey
 * @Date: 2019/3/30
 * 网关对post,get请求等添加的参数
 */
public class RequestAddParamsDto {

    private Long userId;

    public RequestAddParamsDto() {
        super();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString(){
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }

}
