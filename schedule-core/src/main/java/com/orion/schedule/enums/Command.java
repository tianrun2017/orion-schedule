package com.orion.schedule.enums;


/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/5/30 9:53
 * @Version 1.0.0
 */
public enum Command {

    /**
     * 心跳消息
     */
    PING,
    /**
     * 第一次调度
     */
    SCHEDULE,
    /**
     * 正常调度后取值
     */
    RUN,
    /**
     * 服务端下发任务终止指令
     */
    SCHEDULE_STOP,
    /**
     * 任务终止
     */
    STOP;
}
