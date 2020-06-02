package com.orion.schedule.progress.dao;


import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.orion.schedule.config.DataSourceConfig;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.progress.util.ScheduleEncrypt;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/8 22:54
 * @Version 1.0.0
 */
public class SchedulePersistent {


    @Autowired
    private ScheduleServerConfig scheduleServerConfig;

    private DruidDataSource druidDataSource;

    public DruidDataSource buildDataSource() throws Exception {
        DataSourceConfig dataSource = scheduleServerConfig.getTask().getDataSource();
        druidDataSource = new DruidDataSource();
        if (StringUtils.isNotEmpty(dataSource.getToken())) {
            String decrypt = ScheduleEncrypt.decrypt(dataSource.getToken());
            JSONObject jsonObject = JSON.parseObject(decrypt);
            druidDataSource.setUrl(jsonObject.getString("url"));
            druidDataSource.setUsername(jsonObject.getString("user"));
            druidDataSource.setPassword(jsonObject.getString("pwd"));
        } else {
            druidDataSource.setUsername(ScheduleEncrypt.decrypt(dataSource.getUserName()));
            druidDataSource.setPassword(ScheduleEncrypt.decrypt(dataSource.getPassword()));
            druidDataSource.setUrl(ScheduleEncrypt.decrypt(dataSource.getUrl()));
        }
        druidDataSource.setMaxActive(dataSource.getPollSize());
        druidDataSource.setDefaultAutoCommit(false);
        druidDataSource.init();
        return druidDataSource;
    }

    public DruidDataSource getDruidDataSource() {
        return druidDataSource;
    }
}
