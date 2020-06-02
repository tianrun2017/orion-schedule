package com.orion.schedule.processor.grid;

import com.alibaba.fastjson.JSON;
import com.orion.schedule.client.ConnectionInstance;
import com.orion.schedule.context.TaskContextUtils;
import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.enums.Command;
import com.orion.schedule.processor.JobProcessor;
import com.orion.schedule.service.ConnectionManagerService;
import com.orion.schedule.util.ScheduleContextUtils;
import com.orion.schedule.util.StepTraceIdUtils;
import com.orion.schedule.util.TaskNodeUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/7/25 14:42
 * @Version 1.0.0
 */
public abstract class GridJobProcessor implements JobProcessor {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * @return
     * @parama
     */
    @Override
    public int dispatchData(ScheduleTaskMsg scheduleTaskMsg, List taskData) {
        if (CollectionUtils.isEmpty(taskData)) {
            logger.error("dispatch data should not empty");
            return 0;
        }
        if (!TaskContextUtils.stateNormal(scheduleTaskMsg)) {
            logger.info("suspend the task [{}] ", JSON.toJSONString(scheduleTaskMsg));
            return 0;
        }
        ScheduleTaskMsg instanceTasmKsg = new ScheduleTaskMsg<>();
        instanceTasmKsg.setCommand(Command.RUN);
        instanceTasmKsg.setCommandId(scheduleTaskMsg.getCommandId());
        instanceTasmKsg.setProcessor(scheduleTaskMsg.getProcessor());
        instanceTasmKsg.setCurrentNodeName(TaskNodeUtil.nextLevel(scheduleTaskMsg.getCurrentNodeName()));
        instanceTasmKsg.setStartDate(scheduleTaskMsg.getStartDate());
        instanceTasmKsg.setStartHost(scheduleTaskMsg.getStartHost());
        instanceTasmKsg.setStepHost(scheduleTaskMsg.getStartHost());
        instanceTasmKsg.setTaskContext(scheduleTaskMsg.getTaskContext());
        instanceTasmKsg.setTaskDataList(taskData);
        instanceTasmKsg.setTaskId(scheduleTaskMsg.getTaskId());
        instanceTasmKsg.setScheduleTraceId(scheduleTaskMsg.getScheduleTraceId());
        instanceTasmKsg.setStepTraceId(StepTraceIdUtils.traceId());
        ConnectionManagerService connectionManagerService = ScheduleContextUtils.getBean(ConnectionManagerService.class);
        ConnectionInstance connectionInstance = connectionManagerService.selectInstance(scheduleTaskMsg.getGroupCode());
        if (connectionInstance == null) {
            logger.error("not found instance for group {} ", scheduleTaskMsg.getGroupCode());
            return 0;
        }
        //默认策略重试三次
        for (int i = 0; i < 3; i++) {
            try {
                boolean b = connectionInstance.sendData(instanceTasmKsg);
                logger.info("dispatch data to server [{}] ,result [{}]", connectionInstance.getRemoteServer(), b);
                if (b) {
                    return CollectionUtils.size(taskData);
                }
            } catch (Throwable e) {
                logger.error("dispatch data to server " + connectionInstance.getRemoteServer() + " error", e);
            }
        }
        return 0;
    }
}
