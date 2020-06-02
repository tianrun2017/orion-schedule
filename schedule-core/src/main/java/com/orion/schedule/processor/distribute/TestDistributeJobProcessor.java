package com.orion.schedule.processor.distribute;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.orion.schedule.context.TaskContextUtils;
import com.orion.schedule.domain.ScheduleTaskMsg;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/6 9:21
 * @Version 1.0.0
 */
public class TestDistributeJobProcessor extends DistributedJobProcessor {

    @Override
    public Long fetchData(ScheduleTaskMsg scheduleTaskMsg) {
        int cnt = 0;
        JSONObject jsonObject1 = JSON.parseObject(scheduleTaskMsg.getTaskContext());
        Integer times = jsonObject1.getInteger("times");

        List<Object> processData = Lists.newArrayList();
        for (int i = 0; i < times; i++) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", "zhangsan_" + i);
            jsonObject.put("age", new Random().nextInt(100));
            processData.add(jsonObject);
            if (processData.size() == 50) {
                int dispatchCnt = dispatchData(scheduleTaskMsg, processData);
                cnt = cnt + dispatchCnt;
                processData.clear();
            }
        }
        if (processData.size() != 0) {
            int dispatchCnt = dispatchData(scheduleTaskMsg, processData);
            cnt = cnt + dispatchCnt;
            processData.clear();
        }
        return cnt * 1L;
    }

    @Override
    public int processData(ScheduleTaskMsg scheduleTaskMsg) {
        Random random = new Random();
        try {
            JSONObject jsonObject = JSON.parseObject(scheduleTaskMsg.getTaskContext());
            scheduleTaskMsg.getTaskDataList().stream().forEach(param -> {
                if (TaskContextUtils.stateNormal(scheduleTaskMsg)) {
                    logger.info("process data {} ", JSON.toJSONString(param));
                    try {
                        TimeUnit.MILLISECONDS.sleep(random.nextInt(jsonObject.getInteger("sleep")));
                    } catch (InterruptedException e) {
                        logger.error("stop error");
                    }
                }
            });
        } catch (Throwable e) {
            logger.error("sleep error ", e);
        }
        return random.nextInt(scheduleTaskMsg.getTaskDataList().size());
    }

}
