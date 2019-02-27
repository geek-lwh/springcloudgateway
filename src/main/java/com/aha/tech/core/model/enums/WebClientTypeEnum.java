package com.aha.tech.core.model.enums;

/**
 * @Author: luweihong
 * @Date: 2019/2/27
 */
public enum WebClientTypeEnum {

    WEB("web", 1), ANDROID("android", 2), IOS("ios", 3);

    private String name;

    private int value;

    private WebClientTypeEnum(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
