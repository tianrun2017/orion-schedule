package com.orion.schedule.util;


import java.util.UUID;

/**
 * 单点追踪ID生成器
 *
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/7/1 14:49
 * @Version 1.0.0
 */
public class StepTraceIdUtils {


    /**
     * traceId
     *
     * @return
     */
    public static final String traceId() {
        return UUID.randomUUID().toString();
    }
}
