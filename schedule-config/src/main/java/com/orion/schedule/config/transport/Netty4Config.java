package com.orion.schedule.config.transport;

import lombok.Data;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 14:47
 * @Version 1.0.0
 */
@Data
public class Netty4Config {
    int serverPort = 9006;
    int idleTime = 10;
    int connectionTimeOut = 5000;
    //kill时，等待多久关闭
    int closeWait = 5;
}
