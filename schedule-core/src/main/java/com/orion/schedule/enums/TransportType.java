package com.orion.schedule.enums;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/3 16:18
 * @Version 1.0.0
 */
public enum TransportType {
    NETTY4("netty4");
    String code;

    TransportType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
