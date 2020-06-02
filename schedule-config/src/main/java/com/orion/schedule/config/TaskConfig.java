package com.orion.schedule.config;

import lombok.Data;

import java.util.List;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 14:16
 * @Version 1.0.0
 */
@Data
public class TaskConfig {
    List<String> groupList;
    int processThread = 10;
    DataSourceConfig dataSource;
    String notifyType = "KAFKA";
    MessageNotifyConfig messageConfig;

}
