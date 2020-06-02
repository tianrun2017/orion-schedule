package com.orion.schedule.service;

import com.orion.schedule.config.RegisterConfig;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.config.TransportConfig;
import com.orion.schedule.register.ServerRegister;
import com.orion.schedule.register.ServerRegisterService;
import com.orion.schedule.transport.ServerTransport;
import com.orion.schedule.transport.ServerTransportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description if the command shoudnot build this service
 * @Author beedoorwei
 * @Date 2019/5/30 10:46
 * @Version 1.0.0
 */
public class ScheduleServerService {

    @Autowired
    ServerRegisterService serverRegisterService;
    @Autowired
    ServerTransportService serverTransportService;
    private Logger logger = LoggerFactory.getLogger(ScheduleServerService.class);
    @Autowired
    private ScheduleServerConfig scheduleServerConfig;

    public void startServer() throws Exception {
        //start server
        TransportConfig transport = scheduleServerConfig.getTransport();

        ServerTransport serverTransport = serverTransportService
                .serverTransport(transport.getCode());
        serverTransport.startServer();

        //register it self
        RegisterConfig register = scheduleServerConfig.getRegister();
        String registerType = register.getCode();
        ServerRegister serverRegister = serverRegisterService.serverRegister(registerType);
        if (serverRegister == null) {
            logger.error("not found the server ");
            throw new RuntimeException("the register [" + registerType + "] cannot found ");
        }

        serverRegister.init();
        serverRegister.register();

        /**
         * 增加解除注册的能力
         */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serverTransport.cleanTransport();
                serverRegister.unRegister();
            } catch (Throwable e) {
                logger.error("unRegister exception ", e);
            }
        }));
    }
}
