package com.orion.schedule.register.listener;

import com.orion.schedule.register.ServerInstance;

import java.util.List;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/5 11:56
 * @Version 1.0.0
 */
public interface ServerStateChangeListener {

    /**
     * remove server
     *
     * @param serverInstance
     */
    public void serverRemoved(ServerInstance serverInstance);

    /**
     * add server
     *
     * @param serverInstance
     */
    public void serverAdd(ServerInstance serverInstance);

    /**
     * refresh all server
     *
     * @param serverInstanceList
     */
    public void refreshAll(List<ServerInstance> serverInstanceList);
}
