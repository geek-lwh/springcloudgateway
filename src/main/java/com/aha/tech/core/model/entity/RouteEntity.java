package com.aha.tech.core.model.entity;

/**
 * @Author: luweihong
 * @Date: 2019/3/5
 */
public class RouteEntity {

    private String id;

    private String path;

    private String uri;

    private String contextPath;

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
}
