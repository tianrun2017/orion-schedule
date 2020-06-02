package com.orion.schedule.service;

import com.orion.schedule.client.ConnectionInstance;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.transport.ServerTransportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/5 6:36
 * @Version 1.0.0
 */
public class ScheduleClientService {

    @Autowired
    ScheduleServerConfig scheduleServerConfig;
    private Logger logger = LoggerFactory.getLogger(ScheduleClientService.class);
    @Autowired
    private ServerTransportService serverTransportService;

    @Autowired
    private ConnectionManagerService connectionManagerService;

    /**
     * send data
     *
     * @param scheduleTaskMsg
     * @param dataList
     * @param <T>
     * @return
     */
    public <T> boolean sendData(ScheduleTaskMsg scheduleTaskMsg, List<T> dataList) throws Exception {
        String groupCode = scheduleTaskMsg.getGroupCode();
        scheduleTaskMsg.setTaskDataList(dataList);
        ConnectionInstance connectionInstance = connectionManagerService.selectInstance(groupCode);
        if (connectionInstance == null) {
            throw new RuntimeException("there is no connection instance for groupCode" + groupCode);
        }
        return connectionInstance.sendData(scheduleTaskMsg);
    }
}
