package com.orion.schedule.transport;

import com.orion.schedule.client.ConnectionInstance;
import com.orion.schedule.codec.ScheduleCodec;
import com.orion.schedule.codec.ServerCodecService;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.config.transport.Netty4Config;
import com.orion.schedule.enums.TransportType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 15:39
 * @Version 1.0.0
 */
public class Netty4Transport implements ServerTransport {

    @Autowired
    ServerCodecService serverCodecService;
    Netty4Client netty4Client;
    Netty4Server netty4Server;
    @Autowired
    private ScheduleServerConfig scheduleServerConfig;

    @Override
    public boolean startServer() throws Exception {
        Netty4Config netty4Config = scheduleServerConfig.getTransport().getConfig();
        ScheduleCodec codec = serverCodecService.getCodec(scheduleServerConfig.getCodec().getCode());
        netty4Server = Netty4Server.defaultServer()
                .withCodec(codec)
                .withNetty4Config(netty4Config);
        netty4Server.buildServer();
        return true;
    }

    @Override
    public void transportInit() throws Exception {
        Netty4Config netty4Config = scheduleServerConfig.getTransport().getConfig();
        ScheduleCodec codec = serverCodecService.getCodec(scheduleServerConfig.getCodec().getCode());
        netty4Client = Netty4Client.getInstance()
                .withNett4Config(netty4Config)
                .withCodec(codec);
        netty4Client.init();
    }

    @Override
    public void cleanTransport() throws Exception {
        if (netty4Server != null) {
            netty4Server.clean();
        }
    }

    @Override
    public ConnectionInstance connectServer(String remoteServer, int port) {
        return netty4Client.connect(remoteServer, port);
    }

    @Override
    public int transportPort() {
        return scheduleServerConfig.getTransport().getConfig().getServerPort();
    }

    @Override
    public String transportType() {
        return TransportType.NETTY4.getCode();
    }
}
