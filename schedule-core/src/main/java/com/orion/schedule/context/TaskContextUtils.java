package com.orion.schedule.context;

import com.google.common.collect.Maps;
import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.enums.Command;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * we suggest that one task the current validate traceId info may not exceed to 1000 at the same time,
 * <p>
 * data model is {
 * taskId:{
 * commandIdQueue,
 * commandId:{
 * state,
 * traceQueue
 * }
 * }
 * }
 * when task schedule or run ,then init the command info
 *
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/7/2 15:46
 * @Version 1.0.0
 */
public class TaskContextUtils {
    static Map<Long, TaskContext> taskContextMap = Maps.newConcurrentMap();
    private static Logger logger = LoggerFactory.getLogger(TaskContextUtils.class);

    public static boolean stateNormal(ScheduleTaskMsg scheduleTaskMsg) {
        //如果是多级任务，则保持一致性，执行完毕再说
        int level = 0;
        String[] s = StringUtils.split(scheduleTaskMsg.getCurrentNodeName(), "_");
        if (s != null && s.length == 2) {
            level = NumberUtils.toInt(s[1], 0);
        }
        if (scheduleTaskMsg.getCommand() == Command.RUN && level > 1) {
            return true;
        }
        Long taskId = scheduleTaskMsg.getTaskId();
        if (taskContextMap.containsKey(taskId)) {
            TaskContext taskContext = taskContextMap.get(taskId);
            boolean b = taskContext.stateNormal(scheduleTaskMsg.getCommandId());
            logger.debug("stop validate result [{}] is {} {}", scheduleTaskMsg.getStepTraceId(), scheduleTaskMsg.getCommandId(), b);
            return b;

        }
        return true;
    }

    public static void stop(Long taskId, Long commandId) {
        if (taskContextMap.containsKey(taskId)) {
            TaskContext taskContext = taskContextMap.get(taskId);
            taskContext.stop(commandId);
            logger.debug("stop result is {} {}", commandId, taskContext.stateNormal(commandId));
        }
    }

    public static boolean isScheduled(ScheduleTaskMsg scheduleTaskMsg) {
        initContext(scheduleTaskMsg);
        TaskContext taskContext = taskContextMap.get(scheduleTaskMsg.getTaskId());
        return taskContext.validateRepeat(scheduleTaskMsg.getCommandId(), scheduleTaskMsg.getScheduleTraceId());
    }

    public static boolean initContext(ScheduleTaskMsg scheduleTaskMsg) {
        Long taskId = scheduleTaskMsg.getTaskId();
        if (!taskContextMap.containsKey(taskId)) {
            synchronized (TaskContextUtils.class) {
                if (!taskContextMap.containsKey(taskId)) {
                    TaskContext taskContext = new TaskContext();
                    taskContextMap.put(taskId, taskContext);
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean validateAndInit(ScheduleTaskMsg scheduleTaskMsg) {
        initContext(scheduleTaskMsg);
        TaskContext taskContext = taskContextMap.get(scheduleTaskMsg.getTaskId());
        return taskContext.validateRepeat(scheduleTaskMsg.getCommandId(), scheduleTaskMsg.getStepTraceId());
    }
}
