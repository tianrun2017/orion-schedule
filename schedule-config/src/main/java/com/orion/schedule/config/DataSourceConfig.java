package com.orion.schedule.config;

import lombok.Data;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/8 23:09
 * @Version 1.0.0
 */
@Data
public class DataSourceConfig implements java.io.Serializable {
    String url;
    String userName;
    String password;
    String token;
    int pollSize = 5;
}
