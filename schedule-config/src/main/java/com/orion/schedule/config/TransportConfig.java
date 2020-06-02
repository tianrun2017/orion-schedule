package com.orion.schedule.config;

import com.orion.schedule.config.transport.Netty4Config;
import lombok.Data;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 14:17
 * @Version 1.0.0
 */
@Data
public class TransportConfig {
    String code = "netty4";
    Netty4Config config;
}
