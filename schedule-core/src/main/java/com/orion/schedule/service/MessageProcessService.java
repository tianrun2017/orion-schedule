package com.orion.schedule.service;

import com.alibaba.fastjson.JSON;
import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.domain.TaskQueue;
import com.orion.schedule.enums.Command;
import com.orion.schedule.processor.JobProcessor;
import com.orion.schedule.processor.JobProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/5/31 18:37
 * @Version 1.0.0
 */
public class MessageProcessService {

    private Logger logger = LoggerFactory.getLogger(MessageProcessService.class);

    @Autowired
    private JobProcessorService jobProcessorService;

    private TaskQueue queue;

    public void setQueue(TaskQueue queue) {
        this.queue = queue;
    }

    /**
     * 处理任务
     *
     * @param scheduleTaskMsg
     */
    public void process(ScheduleTaskMsg scheduleTaskMsg) {
        if (scheduleTaskMsg.getScheduleTraceId() == null || scheduleTaskMsg.getCommandId() == null
                || scheduleTaskMsg.getTaskId() == null) {
            logger.warn("the taskId,scheduleTraceId,commandId must not null [{}] ", JSON.toJSONString(scheduleTaskMsg));
            return;
        }

        try {
            JobProcessor jobProcessor = jobProcessorService.getProcessor(scheduleTaskMsg.getProcessor());
            if (jobProcessor == null) {
                logger.error("[{}] processor not found [{}] ", scheduleTaskMsg.getScheduleTraceId(), scheduleTaskMsg.getProcessor());
                return;
            }
            Command command = scheduleTaskMsg.getCommand();
            switch (command) {
                case SCHEDULE:
                case RUN:
                    queue.put(scheduleTaskMsg);
                    break;
                case SCHEDULE_STOP:
                case STOP:
                    queue.putFirst(scheduleTaskMsg);
                    break;
                default:
                    logger.info("[{}] command not found [{}] ", scheduleTaskMsg.getScheduleTraceId(), scheduleTaskMsg.getCommand());
            }

        } catch (Throwable e) {
            logger.error(" message process error " + JSON.toJSONString(scheduleTaskMsg), e);
        }
    }
}
