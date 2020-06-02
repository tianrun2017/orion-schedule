package com.orion.schedule.processor.standalone;

import com.alibaba.fastjson.JSON;
import com.orion.schedule.config.progress.TaskExecLogService;
import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.processor.JobProcessor;
import com.orion.schedule.util.ScheduleContextUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/5/31 22:21
 * @Version 1.0.0
 */
public abstract class StandaloneJobProcessor implements JobProcessor {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Long fetchData(ScheduleTaskMsg scheduleTaskMsg) {
        TaskExecLogService taskExecLogService = ScheduleContextUtils.getBean(TaskExecLogService.class);

        int suc = 0, fail = 0;
        try {
            Pair<Integer, Integer> process = process(scheduleTaskMsg);
            suc = process.getLeft();
            fail = process.getRight();
            taskExecLogService.updateRunResult(scheduleTaskMsg.getCommandId(), suc, fail);
        } catch (Throwable e) {
            logger.error("process data error [{}] " + JSON.toJSONString(scheduleTaskMsg), e);
        }
        return (suc + fail) * 1L;
    }

    /**
     * process,
     *
     * @param scheduleTaskMsg
     * @return left key is the success count ,and right value is fail count value
     */
    public abstract Pair<Integer, Integer> process(ScheduleTaskMsg scheduleTaskMsg);

    @Override
    public int processData(ScheduleTaskMsg scheduleTaskMsg) {
        return 0;
    }

    /**
     * @return
     * @parama
     */
    @Override
    public int dispatchData(ScheduleTaskMsg scheduleTaskMsg, List taskData) throws Exception {
        return 0;
    }

}

