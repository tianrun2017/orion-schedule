package com.orion.schedule.service;

import com.alibaba.fastjson.JSON;
import com.orion.schedule.config.MessageNotifyConfig;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.config.progress.OperType;
import com.orion.schedule.config.progress.ProgressListener;
import com.orion.schedule.progress.dao.DBExecLogService;
import com.orion.schedule.progress.kafka.KafkaMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/12/17 22:32
 * @Version 1.0.0
 */

public class ProgressProcessService {

    @Autowired
    DBExecLogService dbExecLogService;
    private Logger logger = LoggerFactory.getLogger(ProgressProcessService.class);
    @Autowired
    private ScheduleServerConfig scheduleServerConfig;

    public void init() {
        ProgressListener progressListener = buildListener();
        MessageNotifyConfig messageConfig = scheduleServerConfig.getTask().getMessageConfig();
        KafkaMessageConsumer kafkaMessageConsumer = new KafkaMessageConsumer();
        kafkaMessageConsumer.initConsumer(scheduleServerConfig.getTask().getMessageConfig().isUseSsl(), messageConfig.getTopic(), messageConfig.getRegister(), progressListener);
        logger.info("kafka consumer init success");
    }

    public ProgressListener buildListener() {
        return processMsgDto -> {
            try {
                logger.info("receive mq message {} ", JSON.toJSONString(processMsgDto));
                OperType ins = OperType.ins(processMsgDto.getType());
                switch (ins) {
                    case STOP:
                        checkNotNull("stop must have commandId and taskId ", processMsgDto.getCommandId(), processMsgDto.getTaskId());
                        dbExecLogService.updateStopState(processMsgDto.getCommandId(), processMsgDto.getTaskId());
                        break;
                    case FETCH_FINISH:
                        checkNotNull("fetch must have commandId and total ", processMsgDto.getCommandId(), processMsgDto.getTotal());
                        dbExecLogService.updateFetchResult(processMsgDto.getCommandId(), processMsgDto.getTotal());
                        break;
                    case BATCH_FINNISH:
                        checkNotNull("batch must have commandId ,fail and suc ", processMsgDto.getCommandId(), processMsgDto.getSuc(), processMsgDto.getFail());
                        dbExecLogService.updateRunResult(processMsgDto.getCommandId(), processMsgDto.getSuc().intValue(), processMsgDto.getFail().intValue());
                        break;
                }
            } catch (Throwable e) {
                logger.error("progress process exception ", e);
            }
        };
    }

    public void checkNotNull(String msg, Object... param) {
        if (param.length != 0) {
            for (Object o : param) {
                if (o == null) {
                    throw new IllegalArgumentException(msg);
                }
            }
        }
    }
}
