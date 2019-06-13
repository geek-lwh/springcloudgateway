package com.aha.tech.core.model.entity;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @Author: luweihong
 * @Date: 2019/3/5
 */
public class RouteEntity {

    /**
     * 路由id
     */
    private String id;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 转发路径
     */
    private String uri;

    /**
     * java项目的contextPath
     */
    private String contextPath;

    /**
     * 重写后的路径
     */
    private String rewritePath;

    /**
     * 接口每秒产生多少令牌
     */
    private Integer replenishRate;

    /**
     * 接口总共存储多少令牌
     */
    private Integer burstCapacity;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getRewritePath() {
        return rewritePath;
    }

    public void setRewritePath(String rewritePath) {
        this.rewritePath = rewritePath;
    }

    public Integer getReplenishRate() {
        return replenishRate;
    }

    public void setReplenishRate(Integer replenishRate) {
        this.replenishRate = replenishRate;
    }

    public Integer getBurstCapacity() {
        return burstCapacity;
    }

    public void setBurstCapacity(Integer burstCapacity) {
        this.burstCapacity = burstCapacity;
    }

    @Override
    public String toString(){
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}
