package com.orion.schedule.register;

import com.netflix.appinfo.PropertiesInstanceConfig;
import com.orion.schedule.common.util.InetUtils;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2020/7/2 19:56
 * @Version 1.0.0
 */
public class ScheduleEurekaInstance extends PropertiesInstanceConfig {
    private String appName;
    private String ip;
    private int port;

    public ScheduleEurekaInstance(String appName, String ip, int port) {
        this.appName = appName;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String getInstanceId() {
        return String.format("%s:%s", InetUtils.getSelfIp(), port);
    }

    @Override
    public String getAppname() {
        return this.appName;
    }

    @Override
    public boolean isInstanceEnabledOnit() {
        return true;
    }

    @Override
    public int getLeaseRenewalIntervalInSeconds() {
        return 1;
    }

    @Override
    public int getLeaseExpirationDurationInSeconds() {
        return 3;
    }
}
