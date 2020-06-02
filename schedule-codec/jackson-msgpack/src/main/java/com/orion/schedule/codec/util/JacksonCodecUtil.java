package com.orion.schedule.codec.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/5/31 16:12
 * @Version 1.0.0
 */
public class JacksonCodecUtil {

    private static JacksonCodecUtil jacksonCodecUtil = new JacksonCodecUtil();
    ThreadLocal<ObjectMapper> mapperThreadLocal = new ThreadLocal<>();

    private JacksonCodecUtil() {
    }

    public static JacksonCodecUtil getInstance() {
        return jacksonCodecUtil;
    }

    public ObjectMapper getMapper() {
        if (mapperThreadLocal.get() == null) {
            ObjectMapper value = new ObjectMapper(new MessagePackFactory());
            value.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapperThreadLocal.set(value);
        }
        return mapperThreadLocal.get();
    }
}

