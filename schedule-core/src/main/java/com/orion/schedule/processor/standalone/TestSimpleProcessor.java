package com.orion.schedule.processor.standalone;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.orion.schedule.domain.ScheduleTaskMsg;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/5/31 23:58
 * @Version 1.0.0
 */
@Component
public class TestSimpleProcessor extends StandaloneJobProcessor {

    private Logger logger = LoggerFactory.getLogger(TestSimpleProcessor.class);

    @Override
    public Pair<Integer, Integer> process(ScheduleTaskMsg scheduleTaskMsg) {
        JSONObject jsonObject = JSON.parseObject(scheduleTaskMsg.getTaskContext());
        Random random = new Random();
        Integer times = jsonObject.getInteger("times");
        for (int i = 0; i < times; i++) {
            try {
                logger.info("process data {}");
                TimeUnit.MILLISECONDS.sleep(random.nextInt(jsonObject.getInteger("sleep")));
            } catch (InterruptedException e) {
                logger.error("execute error");
            }
        }
        int i = random.nextInt(scheduleTaskMsg.getTaskDataList().size());
        return Pair.of(i, scheduleTaskMsg.getTaskDataList().size() - i);
    }
}
