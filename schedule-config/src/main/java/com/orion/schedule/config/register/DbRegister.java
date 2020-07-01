package com.orion.schedule.config.register;

import com.orion.schedule.config.DataSourceConfig;
import lombok.Data;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2020/7/1 10:45
 * @Version 1.0.0
 */
@Data
public class DbRegister extends DataSourceConfig {

    /**
     *
     */
    private int ttl = 500;
}
