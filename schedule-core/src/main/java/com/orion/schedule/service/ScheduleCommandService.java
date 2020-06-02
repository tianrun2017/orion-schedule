package com.orion.schedule.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.orion.schedule.client.ConnectionInstance;
import com.orion.schedule.domain.ScheduleTaskMsg;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/5 15:48
 * @Version 1.0.0
 */
public class ScheduleCommandService {

    private Logger logger = LoggerFactory.getLogger(ScheduleCommandService.class);

    @Autowired
    private ConnectionManagerService connectionManagerService;


    /**
     * command with random remote server
     *
     * @param scheduleTaskMsg
     * @return
     * @throws Exception
     */
    public Pair<Boolean, String> command(ScheduleTaskMsg scheduleTaskMsg) throws Exception {
        return command(scheduleTaskMsg, null);
    }

    /**
     * send command
     *
     * @param scheduleTaskMsg
     * @return
     */
    public Pair<Boolean, String> command(ScheduleTaskMsg scheduleTaskMsg, String server) throws Exception {
        scheduleTaskMsg.setTaskDataList(Lists.newArrayList());
        final ConnectionInstance connectionInstance = connectionManagerService.selectInstance(scheduleTaskMsg.getGroupCode(), server);
        if (connectionInstance == null) {
            throw new RuntimeException("there is no connection instance " + JSON.toJSONString(scheduleTaskMsg));
        }
        //retry 3 times to one server ,if the it fail
        for (int i = 0; i < 3; i++) {
            boolean b = connectionInstance.sendCommand(scheduleTaskMsg);
//            b = connectionInstance.sendCommand(scheduleTaskMsg);
            logger.info("send command [{}] to client [{}],result is [{}]", JSON.toJSONString(scheduleTaskMsg), connectionInstance.getRemoteServer(), b);
            if (b) {
                return Pair.of(true, connectionInstance.getRemoteServer());
            } else {
                logger.warn("[NOTIFY] send command error [{}] context is [{}]", connectionInstance.getRemoteServer(), JSON.toJSONString(scheduleTaskMsg));
            }
        }
        logger.warn("[NOTIFY] send command [{}] to server [{}] try 3 times error ", JSON.toJSONString(scheduleTaskMsg), connectionInstance.getRemoteServer());
        return Pair.of(false, connectionInstance.getRemoteServer());
    }
}
