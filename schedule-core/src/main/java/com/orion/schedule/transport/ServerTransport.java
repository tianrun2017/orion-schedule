package com.orion.schedule.transport;

import com.orion.schedule.client.ConnectionInstance;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 15:21
 * @Version 1.0.0
 */
public interface ServerTransport {
    /**
     * do register user
     */
    public boolean startServer() throws Exception;


    /**
     * the connection init
     *
     * @return
     * @throws Exception
     */
    public void transportInit() throws Exception;

    /**
     * clean the connection
     *
     * @throws Exception
     */
    public void cleanTransport() throws Exception;

    /**
     * do connect the server
     *
     * @param remoteServer
     * @param port
     * @return
     */
    public ConnectionInstance connectServer(String remoteServer, int port);

    /**
     * return the transport port
     *
     * @return
     */
    public int transportPort();

    /**
     * get the transport name
     *
     * @return
     */
    public String transportType();
}
