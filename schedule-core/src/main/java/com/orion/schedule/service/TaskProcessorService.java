package com.orion.schedule.service;

import com.alibaba.fastjson.JSON;
import com.orion.schedule.client.ConnectionInstance;
import com.orion.schedule.common.util.UUIDUtils;
import com.orion.schedule.config.progress.TaskExecLogService;
import com.orion.schedule.context.TaskContextUtils;
import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.domain.TaskQueue;
import com.orion.schedule.enums.Command;
import com.orion.schedule.processor.JobProcessor;
import com.orion.schedule.processor.JobProcessorService;
import com.orion.schedule.util.ScheduleContextUtils;
import com.orion.schedule.util.TaskNodeUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/5/31 18:37
 * @Version 1.0.0
 */
public class TaskProcessorService {

    private Logger logger = LoggerFactory.getLogger(TaskProcessorService.class);

    @Autowired
    private JobProcessorService jobProcessorService;

    @Autowired
    private TaskExecLogService taskExecLogService;

    @Autowired
    private MessageProcessService messageProcessService;

    private TaskQueue<ScheduleTaskMsg> queue;

    private int workThread = 10;
    private int queueSize = 100;


    public void init() {
        queue = new TaskQueue<>(queueSize);
        messageProcessService.setQueue(queue);

        ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(workThread,
                new BasicThreadFactory.Builder().namingPattern("schedule-processor-%d").priority(10).daemon(true).build());
        for (int i = 0; i < workThread; i++) {
            threadPoolExecutor.submit((Runnable) () -> {
                while (true) {
                    try {
                        ScheduleTaskMsg take = queue.take();
                        process(take);
                    } catch (Throwable e) {
                        logger.error("process msg error ", e);
                    }
                }

            });
        }
    }

    /**
     * 处理任务
     *
     * @param scheduleTaskMsg
     */
    public void process(ScheduleTaskMsg scheduleTaskMsg) {

        try {
            JobProcessor jobProcessor = jobProcessorService.getProcessor(scheduleTaskMsg.getProcessor());
            Command command = scheduleTaskMsg.getCommand();
            switch (command) {
                case SCHEDULE:
                    scheduleTask(jobProcessor, scheduleTaskMsg);
                    break;
                case RUN:
                    processData(jobProcessor, scheduleTaskMsg);
                    break;
                case SCHEDULE_STOP:
                    dispatchStop(scheduleTaskMsg);
                    break;
                case STOP:
                    stop(scheduleTaskMsg);
                    break;
            }
        } catch (Throwable e) {
            logger.error("[NOTIFY] process data error " + JSON.toJSONString(scheduleTaskMsg), e);
        } finally {
            logger.info("[{}] command [{}] step [{}] finish", scheduleTaskMsg.getScheduleTraceId(), scheduleTaskMsg.getCommand(), scheduleTaskMsg.getStepTraceId());
        }
    }

    /**
     * stop task
     *
     * @param scheduleTaskMsg
     */
    private void stop(ScheduleTaskMsg scheduleTaskMsg) {
        TaskContextUtils.stop(scheduleTaskMsg.getTaskId(), scheduleTaskMsg.getParentCommandId());
    }

    /**
     * schedule task
     *
     * @param jobProcessor
     * @param scheduleTaskMsg
     */
    private void scheduleTask(JobProcessor jobProcessor, ScheduleTaskMsg scheduleTaskMsg) {
        boolean scheduled = TaskContextUtils.isScheduled(scheduleTaskMsg);
        if (scheduled) {
            logger.error("[NOTIFY] the task command [" + scheduleTaskMsg.getCommandId() + "] has been scheduled : " + scheduleTaskMsg.getCommand() + "\t" + scheduleTaskMsg.getScheduleTraceId());
            return;
        }
        fetchData(jobProcessor, scheduleTaskMsg);
    }

    /**
     * 发送任务终止指令
     */
    private void dispatchStop(ScheduleTaskMsg scheduleTaskMsg) throws Exception {
        ConnectionManagerService connectionManagerService = ScheduleContextUtils
                .getBean(ConnectionManagerService.class);
        List<ConnectionInstance> connectionInstanceList = connectionManagerService
                .selectAllInstance(scheduleTaskMsg.getGroupCode());
        if (!connectionInstanceList.isEmpty()) {
            connectionInstanceList.stream().forEach(connectionInstance -> {
//				if (!StringUtils.equals(connectionInstance.getRemoteServer(), InetUtils.getSelfIp())) {
                //下发终止指令
                ScheduleTaskMsg<Object> stopMsg = new ScheduleTaskMsg<>();
                BeanUtils.copyProperties(scheduleTaskMsg, stopMsg);
                stopMsg.setCommand(Command.STOP);
                stopMsg.setCurrentNodeName(TaskNodeUtil.nextLevel(scheduleTaskMsg.getCurrentNodeName()));
                stopMsg.setStepTraceId(UUIDUtils.next());
                //retry 3 times to one server ,if the it fail
                for (int i = 0; i < 3; i++) {
                    try {
                        boolean b = connectionInstance.sendCommand(stopMsg);
                        logger.info("send command [{}] to client [{}],result is [{}]", JSON.toJSONString(stopMsg), connectionInstance.getRemoteServer(), b);
                        if (b) {
                            break;
                        }
                    } catch (Throwable e) {
                        logger.error("[NOTIFY] send stop command to " + connectionInstance.getRemoteServer() + " exception", e);
                    }
                }
            });
        }
        taskExecLogService.updateStopState(scheduleTaskMsg.getCommandId(), scheduleTaskMsg.getTaskId());
    }

    /**
     * record the task run result
     *
     * @param jobProcessor
     * @param scheduleTaskMsg
     * @throws Exception
     */
    private void processData(JobProcessor jobProcessor, ScheduleTaskMsg scheduleTaskMsg) throws Exception {
        boolean repeatRequest = TaskContextUtils.validateAndInit(scheduleTaskMsg);
        if (repeatRequest) {
            logger.error("repeat request [{}] ", JSON.toJSONString(scheduleTaskMsg));
            return;
        }
        Integer fail = CollectionUtils.size(scheduleTaskMsg.getTaskDataList()), suc = 0;
        try {
            if (CollectionUtils.isNotEmpty(scheduleTaskMsg.getTaskDataList())) {
                suc = jobProcessor.processData(scheduleTaskMsg);
                fail = fail - suc;
            }
        } catch (Throwable e) {
            logger.error("[NOTIFY] process data error [{}] " + JSON.toJSONString(scheduleTaskMsg), e);
        }
        logger.info("add batch [{}] [{}] [{}] [{}] [{}]", scheduleTaskMsg.getCommandId(), scheduleTaskMsg.getStepTraceId(), CollectionUtils.size(scheduleTaskMsg.getTaskDataList()), suc, fail);
        if (TaskNodeUtil.isFirstLevel(scheduleTaskMsg.getCurrentNodeName())) {
            taskExecLogService.updateRunResult(scheduleTaskMsg.getCommandId(), suc, fail);
        }
    }

    /**
     * get the task data
     *
     * @param jobProcessor
     * @param scheduleTaskMsg
     * @throws Exception
     */
    private void fetchData(JobProcessor jobProcessor, ScheduleTaskMsg scheduleTaskMsg) {
        try {
            Long totalData = jobProcessor.fetchData(scheduleTaskMsg);
            if (totalData <= 0) {
                logger.error("commandId [%s] fetch size zero ,task state may not correct ", scheduleTaskMsg.getCommandId());
            }
            logger.info("task [{}] all data [{}]  ", scheduleTaskMsg.getCommandId(), totalData);
            boolean b = taskExecLogService.updateFetchResult(scheduleTaskMsg.getCommandId(), totalData);
            if (!b) {
                logger.warn("task [{}] dispatch and fetch [{}] no equal", scheduleTaskMsg.getCommandId(), totalData);
//            } else {
//                taskExecLogService.cleanRunState(scheduleTaskMsg.getCommandId());
            }
        } catch (Throwable e) {
            logger.error("[NOTIFY] fetch data error  " + JSON.toJSONString(scheduleTaskMsg), e);
        }
    }

}
