package com.orion.schedule.processor;

import com.orion.schedule.domain.ScheduleTaskMsg;

import java.util.List;

public interface JobProcessor {

    /**
     * get the data.when taskCommand is SCHEDULE,then invoke this method
     *
     * @param scheduleTaskMsg
     * @return
     */
    default Long fetchData(ScheduleTaskMsg scheduleTaskMsg) {
        return 0L;
    }

    /**
     * dispatch data
     *
     * @param scheduleTaskMsg
     * @param taskData
     */
    int dispatchData(ScheduleTaskMsg scheduleTaskMsg, List<Object> taskData) throws Exception;

    /**
     * the logic of task
     *
     * @return return the success process count
     */
    int processData(ScheduleTaskMsg scheduleTaskMsg);

}
