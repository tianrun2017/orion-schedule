package com.orion.schedule.codec;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 15:51
 * @Version 1.0.0
 */

import com.orion.schedule.domain.ScheduleTaskMsg;

public interface ScheduleCodec {

    /**
     * encode
     * @param scheduleTaskMsg
     * @return
     * @throws Exception
     */
    public byte[] encode(ScheduleTaskMsg scheduleTaskMsg) throws Exception;

    /**
     * decode
     * @param data
     * @return
     * @throws Exception
     */
    public ScheduleTaskMsg decode(byte[] data) throws Exception;


    public String codecName();

}
