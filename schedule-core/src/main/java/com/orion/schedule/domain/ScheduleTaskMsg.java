package com.orion.schedule.domain;

import com.orion.schedule.enums.Command;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/5/30 9:50
 * @Version 1.0.0
 */

@Data
public class ScheduleTaskMsg<T> {
    /************************任务配置信息，来源于任务配置平台********************/
    /**
     * 任务处理器
     */
    String processor;
    /**
     * 任务上下文
     */
    String taskContext;

    /**
     * 任务分组
     */
    String groupCode;

    /*************************任务调度平台，被调度时产生************************/
    /**
     * 任务指令，第一次调度,主要由调度中心产生
     */
    Command command;
    /**
     * 命令Id
     */
    Long commandId;

    /**
     * 父指令ID, schedule_stop時用到
     */
    Long parentCommandId;

    /**
     * 调度跟踪ID，第一次发起调度时填充
     */
    String scheduleTraceId;

    /**
     * 任务ID
     */
    Long taskId;
    /**
     * 原发起host
     */
    String startHost;


    /***************************任务执行数据*********************************/
    /**
     * 如果有二阶段任务，则此字段标记本次调度的发起服务
     */
    String stepHost;
    /**
     * 阶段ID，二级，三级，四级任务标记为对应的任务StepId
     * 如同一个任务，每次调度stepId会不同，取数逻辑每次派发时taskId一致，stepTraceId则逐步增加
     */
    Long stepId;
    /**
     * 父任务ID,暂时不用，保留后续多级任务扩展用
     */
    Long parentStepId;
    /**
     * 当次任务跟踪id，用于重复请求校验等场景
     */
    String stepTraceId;
    /**
     * 当前节点名称，保留字段，如需要多级分发，则词字段有含义
     */
    String currentNodeName;
    /**
     * 调度发起时间
     */
    Date startDate;
    /**
     * 待执行任务的原始数据，如从数据库，ftp服务器采集的数据
     */
    List<T> taskDataList;
}
