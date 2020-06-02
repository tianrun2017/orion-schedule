package com.orion.schedule.client;

import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.enums.ConnectionState;

/**
 * use this.because we want to remove the ideal connection when
 *
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/7/18 14:38
 * @Version 1.0.0
 */
public class ConnectionProxy implements ConnectionInstance {
    ConnectionInstance connectionInstance;
    Long lastVisit = 0L;
    Long markTime = 0L;

    public static ConnectionProxy proxy(ConnectionInstance connectionInstance) {
        ConnectionProxy connectionProxy = new ConnectionProxy();
        connectionProxy.connectionInstance = connectionInstance;
        return connectionProxy;
    }

    public Long getLastVisit() {
        return lastVisit;
    }

    public Long getMarkTime() {
        return markTime;
    }

    public void setMarkTime(Long markTime) {
        this.markTime = markTime;
    }

    @Override
    public boolean sendData(ScheduleTaskMsg scheduleTaskMsg) throws Exception {
        this.lastVisit = System.currentTimeMillis();
        return connectionInstance.sendData(scheduleTaskMsg);
    }

    @Override
    public boolean sendCommand(ScheduleTaskMsg scheduleTaskMsg) throws Exception {
        this.lastVisit = System.currentTimeMillis();
        return connectionInstance.sendCommand(scheduleTaskMsg);
    }

    @Override
    public boolean disConnection() {
        this.lastVisit = System.currentTimeMillis();
        return connectionInstance.disConnection();
    }

    @Override
    public ConnectionState getConnectionState() {
        this.lastVisit = System.currentTimeMillis();
        return connectionInstance.getConnectionState();
    }

    @Override
    public void stateTrans(ConnectionState newState) {
        connectionInstance.stateTrans(newState);
    }

    @Override
    public String getRemoteServer() {
        this.lastVisit = System.currentTimeMillis();
        return connectionInstance.getRemoteServer();
    }

}
