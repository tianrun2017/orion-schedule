package com.orion.schedule.config;

import com.orion.schedule.config.register.EtcdRegister;
import com.orion.schedule.config.register.NacosRegister;
import lombok.Data;

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

    NacosRegister config;

    EtcdRegister etcdConfig;
}
