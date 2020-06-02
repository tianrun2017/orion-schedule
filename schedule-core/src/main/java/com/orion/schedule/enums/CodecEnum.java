package com.orion.schedule.enums;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/5 19:08
 * @Version 1.0.0
 */
public enum CodecEnum {
    MSGPACK("jackson-msg-pack");
    String code;

    CodecEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
