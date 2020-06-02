package com.orion.schedule.config.register;

import lombok.Data;

import java.util.List;

/**
 * @Description
 * @Author sevenzhong
 * @Date 2019/10/10 14:54
 */

@Data
public class EtcdRegister {
    private List<String> serverList;
    private int ttl;
}
