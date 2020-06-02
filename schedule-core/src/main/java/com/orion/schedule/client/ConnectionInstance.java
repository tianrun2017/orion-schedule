package com.orion.schedule.client;

import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.enums.ConnectionState;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/5 6:50
 * @Version 1.0.0
 */
public interface ConnectionInstance {

    /**
     * send connection data
     *
     * @param scheduleTaskMsg the data config
     * @return
     */
    boolean sendData(ScheduleTaskMsg scheduleTaskMsg) throws Exception;


    /**
     * dispath task command to client
     *
     * @param scheduleTaskMsg
     * @return
     */
    boolean sendCommand(ScheduleTaskMsg scheduleTaskMsg) throws Exception;

    /**
     * disconnectin from server
     *
     * @return
     */
    boolean disConnection();

    /**
     * get the connection state
     *
     * @return
     */
    ConnectionState getConnectionState();

    /**
     * trans to new state,if the old state is removed,then it will hold the REMOVED state for ever
     *
     * @param newState
     */
    void stateTrans(ConnectionState newState);

    /**
     * get the remote server
     *
     * @return
     */
    String getRemoteServer();
}
