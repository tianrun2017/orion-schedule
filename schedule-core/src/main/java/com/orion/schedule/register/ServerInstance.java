package com.orion.schedule.register;

import lombok.Data;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 13:54
 * @Version 1.0.0
 */
@Data
public class ServerInstance {
    public String server;
    public int port;


    public static ServerInstance defaultInstance() {
        return new ServerInstance();
    }

    public ServerInstance withServer(String server) {
        this.server = server;
        return this;
    }

    public ServerInstance withPort(int port) {
        this.port = port;
        return this;
    }
}
