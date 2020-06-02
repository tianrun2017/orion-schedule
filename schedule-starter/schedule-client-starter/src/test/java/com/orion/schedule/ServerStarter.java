package com.orion.schedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/5/30 16:37
 * @Version 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.orion.schedule")
public class ServerStarter {

    public static void main(String[] args) {
        SpringApplication.run(ServerStarter.class, args);
    }
}
