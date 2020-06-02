package com.orion.schedule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 14:51
 * @Version 1.0.0
 */

@Data
@ConfigurationProperties(prefix = "schedule.server")
public class ScheduleServerConfig {
    CodecConfig codec = new CodecConfig();
    TransportConfig transport = new TransportConfig();
    RegisterConfig register = new RegisterConfig();
    TaskConfig task = new TaskConfig();
}
