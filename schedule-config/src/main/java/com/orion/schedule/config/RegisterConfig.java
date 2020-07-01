package com.orion.schedule.config;

import com.orion.schedule.config.register.EtcdRegister;
import com.orion.schedule.config.register.NacosRegister;
import com.orion.schedule.config.register.ZookeeperRegister;
import lombok.Data;

import java.util.List;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 13:59
 * @Version 1.0.0
 */
@Data
public class RegisterConfig {
    /**
     * 注册中心编码
     */
    String code = "nacos";

    /**
     * 注册中心环境
     */
    String registerEnv = "DEVELOP";

    List<String> serverList;

    NacosRegister nacosConfig;

    EtcdRegister etcdConfig;

    ZookeeperRegister zkConfig;

    DataSourceConfig dbConfig;

}

