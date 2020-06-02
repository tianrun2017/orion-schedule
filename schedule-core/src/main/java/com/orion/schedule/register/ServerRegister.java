package com.orion.schedule.register;


import com.orion.schedule.register.listener.ServerStateChangeListener;

import java.util.List;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/5/31 16:37
 * @Version 1.0.0
 */
public interface ServerRegister {

    /**
     * init invoke
     *
     * @return
     * @throws Exception
     */
    public void init() throws Exception;

    /**
     * do register user
     */
    public boolean register() throws Exception;


    /**
     * 解除服务注册
     *
     * @return
     * @throws Exception
     */
    public boolean unRegister() throws Exception;

    /**
     * get all server
     *
     * @return
     */
    public List<ServerInstance> getAllServer(String groupId) throws Exception;


    /**
     * add server change listener
     *
     * @param serverStateChangeListener
     */
    public void addServerChangeListener(String groupId, ServerStateChangeListener serverStateChangeListener) throws Exception;


    /**
     * get the register name
     *
     * @return
     */
    public String registerCode();

}

