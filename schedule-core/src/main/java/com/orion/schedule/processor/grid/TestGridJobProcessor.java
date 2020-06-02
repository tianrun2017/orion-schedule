package com.orion.schedule.processor.grid;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.service.TaskProcessorService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/7/25 14:47
 * @Version 1.0.0
 */
public class TestGridJobProcessor extends GridJobProcessor {
    @Autowired
    private TaskProcessorService taskProcessorService;

    @Override
    public Long fetchData(ScheduleTaskMsg scheduleTaskMsg) {
        int cnt = 0;
        for (int i = 0; i < 50; i++) {
            List<Object> processData = Lists.newArrayList();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", "zhangsan");
            jsonObject.put("age", new Random().nextInt(100));
            processData.add(jsonObject);
            jsonObject = new JSONObject();
            jsonObject.put("name", "zhangsan_" + i);
            jsonObject.put("age", new Random().nextInt(100));
            processData.add(jsonObject);
            int dispatchCnt = dispatchData(scheduleTaskMsg, processData);
            cnt = cnt + dispatchCnt;
            try {
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (Throwable e) {
            } finally {

            }
        }
        return cnt * 1L;
    }

    @Override
    public int processData(ScheduleTaskMsg scheduleTaskMsg) {
        try {
            JSONObject jsonObject = JSON.parseObject(scheduleTaskMsg.getTaskContext());
            String currentNodeName = scheduleTaskMsg.getCurrentNodeName();
            List<Object> nextDispathcList = Lists.newArrayList();
            switch (currentNodeName) {
                case "LEVEL_1":
                    System.out.println("invoke level 1");
                    break;
                case "LEVEL_2":
                    System.out.println("invoke level 2");
                    break;
                case "LEVEL_3":
                    System.out.println("invoke level 3");
                    break;

            }
            //xx业务逻辑
            //如果需要进一步处理
            dispatchData(scheduleTaskMsg, nextDispathcList);
        } catch (Throwable e) {
            logger.error("sleep error ", e);
        }
        return scheduleTaskMsg.getTaskDataList().size();
    }
}
