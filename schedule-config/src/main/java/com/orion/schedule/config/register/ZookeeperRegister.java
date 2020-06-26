package com.orion.schedule.config.register;

import lombok.Data;

import java.util.List;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2020/6/24 21:43
 * @Version 1.0.0
 */
@Data
public class ZookeeperRegister {
    List<String> serverList;
    int timeout = 50000;
}
