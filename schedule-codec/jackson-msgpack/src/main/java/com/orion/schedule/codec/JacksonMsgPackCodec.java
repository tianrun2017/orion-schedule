package com.orion.schedule.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orion.schedule.codec.util.JacksonCodecUtil;
import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.enums.CodecEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/5 19:05
 * @Version 1.0.0
 */
public class JacksonMsgPackCodec implements ScheduleCodec {

    private Logger logger = LoggerFactory.getLogger(JacksonMsgPackCodec.class);

    @Override
    public byte[] encode(ScheduleTaskMsg scheduleTaskMsg) throws Exception {
        ObjectMapper objectMapper = JacksonCodecUtil.getInstance().getMapper();
        byte[] bytes = objectMapper.writeValueAsBytes(scheduleTaskMsg);
        return bytes;
    }

    @Override
    public ScheduleTaskMsg decode(byte[] bytes) throws Exception {
        ScheduleTaskMsg scheduleTaskMsg = JacksonCodecUtil.getInstance().getMapper()
                .readValue(bytes, ScheduleTaskMsg.class);
        return scheduleTaskMsg;
    }

    @Override
    public String codecName() {
        return CodecEnum.MSGPACK.getCode();
    }
}
