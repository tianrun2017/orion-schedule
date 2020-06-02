package com.orion.schedule.progress.kafka;

import com.orion.schedule.config.progress.OperType;
import com.orion.schedule.config.progress.ProcessMsgDto;
import com.orion.schedule.config.progress.TaskExecLogService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/12/16 20:57
 * @Version 1.0.0
 */
public class KafkaExecLogService implements TaskExecLogService {

    @Autowired
    private KafkaMessageProducer kafkaMessageProducer;

    @Override
    public boolean updateRunResult(Long commandId, int success, int fail) throws Exception {
        ProcessMsgDto processMsgDto = ProcessMsgDto.ins(OperType.BATCH_FINNISH).withCommandId(commandId).withSuc(success).withFail(fail);
        return kafkaMessageProducer.sendMsg(commandId, processMsgDto);
    }

    @Override
    public boolean updateFetchResult(Long commandId, Long total) throws Exception {
        ProcessMsgDto processMsgDto = ProcessMsgDto.ins(OperType.FETCH_FINISH).withCommandId(commandId).withTotal(total);
        return kafkaMessageProducer.sendMsg(commandId, processMsgDto);
    }

    @Override
    public void updateStopState(Long commandId, Long taskId) throws Exception {
        ProcessMsgDto processMsgDto = ProcessMsgDto.ins(OperType.STOP).withCommandId(commandId).withTaskId(taskId);
        kafkaMessageProducer.sendMsg(commandId, processMsgDto);
    }
}
