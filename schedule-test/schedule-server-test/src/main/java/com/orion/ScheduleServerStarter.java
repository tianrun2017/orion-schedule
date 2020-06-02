package com.orion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/3 6:19
 * @Version 1.0.0
 */
@SpringBootApplication
public class ScheduleServerStarter implements CommandLineRunner {
    private Logger logger = LoggerFactory.getLogger(ScheduleServerStarter.class);

    public static void main(String[] args) {
        SpringApplication.run(ScheduleServerStarter.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("server start");
    }
}
