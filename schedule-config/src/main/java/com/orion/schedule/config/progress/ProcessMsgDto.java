package com.orion.schedule.config.progress;

import lombok.Data;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/12/17 20:36
 * @Version 1.0.0
 */
@Data
public class ProcessMsgDto {
    int type;
    Long commandId;
    Long taskId;
    Long total;
    Long suc;
    Long fail;

    public static ProcessMsgDto ins(OperType operType) {
        ProcessMsgDto processMsgDto = new ProcessMsgDto();
        processMsgDto.type = operType.getV();
        return processMsgDto;
    }

    public ProcessMsgDto withTaskId(long taskId) {
        this.taskId = taskId;
        return this;
    }

    public ProcessMsgDto withCommandId(long commandId) {
        this.commandId = commandId;
        return this;
    }

    public ProcessMsgDto withTotal(long total) {
        this.total = total;
        return this;
    }

    public ProcessMsgDto withSuc(long suc) {
        this.suc = suc;
        return this;
    }

    public ProcessMsgDto withFail(long fail) {
        this.fail = fail;
        return this;
    }

}

