package com.orion.schedule.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.orion.schedule.client.ConnectionInstance;
import com.orion.schedule.client.ConnectionProxy;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.config.TaskConfig;
import com.orion.schedule.config.TransportConfig;
import com.orion.schedule.enums.ConnectionState;
import com.orion.schedule.register.ServerInstance;
import com.orion.schedule.register.ServerRegister;
import com.orion.schedule.register.ServerRegisterService;
import com.orion.schedule.register.listener.ServerStateChangeListener;
import com.orion.schedule.transport.ServerTransport;
import com.orion.schedule.transport.ServerTransportService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/5 10:58
 * @Version 1.0.0
 */
public class ConnectionManagerService {

    private static ReentrantLock reentrantLock = new ReentrantLock(true);
    @Autowired
    ServerRegisterService serverRegisterService;
    @Autowired
    ServerTransportService serverTransportService;
    /**
     * data module
     * group:
     * serverIp:instance
     */
    Map<String, List<String>> groupConnectionKeyMap = Maps.newConcurrentMap();
    Map<String, ConnectionProxy> connectionInstanceMap = Maps.newConcurrentMap();
    private Logger logger = LoggerFactory.getLogger(ConnectionManagerService.class);
    @Autowired
    private ScheduleServerConfig scheduleServerConfig;
    /**
     * 是否允许迟延创建连接
     */
    private boolean lazyInitConnect = true;

    public ConnectionManagerService(boolean lazyInitConnect) {
        this.lazyInitConnect = lazyInitConnect;
    }

    public void init() throws Exception {
//        addCleanConnection();
        clientInit();
        registerConnection();
        logger.info("connection init suc lazyInit {}", lazyInitConnect);
    }

    /**
     * core logic is that ,first mark if there is not access for this connection
     * when the mark time past 4 minutes but there is no invoke ,clean it
     * clean connection
     */
    private void addCleanConnection() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder().namingPattern("connection-clean-%d").priority(1).daemon(true).build());
        scheduledThreadPoolExecutor.scheduleWithFixedDelay(() -> {
            Iterator<String> iterator = connectionInstanceMap.keySet().iterator();
            while (iterator.hasNext()) {
                String serverKey = iterator.next();
                ConnectionProxy connectionInstance = connectionInstanceMap.get(serverKey);
                long l = System.currentTimeMillis() - connectionInstance.getLastVisit();
                if (l > 1000 * 60 * 5) {
                    if (connectionInstance.getMarkTime() == 0) {
                        connectionInstance.setMarkTime(connectionInstance.getLastVisit());
                    } else {
                        long l1 = connectionInstance.getLastVisit() - connectionInstance.getMarkTime();
                        if (l1 > 1000 * 60 * 4) {
                            //清理连接
                            logger.info("there is no invoke on this connection {} ,remove it ", serverKey);
                            groupConnectionKeyMap.keySet().stream().forEach(s -> {
                                if (StringUtils.isNotEmpty(s)) {
                                    List<String> strings = groupConnectionKeyMap.get(s);
                                    if (strings != null && strings.contains(serverKey)) {
                                        doRemoveConnection(serverKey);
                                        strings.remove(serverKey);
                                    }
                                }
                            });
                        }
                    }
                } else {
                    connectionInstance.setMarkTime(0L);
                }
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    /**
     * client init
     *
     * @throws Exception
     */
    public void clientInit() throws Exception {
        TransportConfig transport = scheduleServerConfig.getTransport();
        ServerTransport serverTransport = serverTransportService
                .serverTransport(transport.getCode());
        serverTransport.transportInit();
    }

    /**
     * init all connection
     */
    public void registerConnection() {
        TaskConfig taskConfig = scheduleServerConfig.getTask();
        Set<String> groupSet = Sets.newHashSet();
        if (CollectionUtils.isEmpty(taskConfig.getGroupList())) {
            return;
        }
        taskConfig.getGroupList().stream().forEach(group -> {
            if (StringUtils.isNotEmpty(group)) {
                groupSet.add(group);
            }
        });
        //init connect
        for (String group : groupSet) {
            initGroup(group);
            logger.info("group {} init suc", group);
        }
    }

    /**
     * get one connection instance
     *
     * @param group
     * @return
     */
    public ConnectionInstance selectInstance(String group, String target) {
        if (!groupConnectionKeyMap.containsKey(group)) {
            initGroup(group);
        }
        List<String> connectionKeyList = groupConnectionKeyMap.get(group);
        if (CollectionUtils.isEmpty(connectionKeyList)) {
            return null;
        }
        String connectionKey = target;
        if (StringUtils.isEmpty(connectionKey)) {
            int selectIdx = RandomUtils.nextInt(0, connectionKeyList.size());
            connectionKey = connectionKeyList.get(selectIdx);
        }
        if (!connectionInstanceMap.containsKey(connectionKey)) {
            initConnection(connectionKey);
        }
        logger.info("select server {}", connectionKey);
        return connectionInstanceMap.get(connectionKey);
    }

    /**
     * get one connection instance
     *
     * @param group
     * @return
     */
    public ConnectionInstance selectInstance(String group) {
        return selectInstance(group, null);
    }

    /**
     * get one connection instance
     *
     * @param group
     * @return
     */
    public List<ConnectionInstance> selectAllInstance(String group) {
        if (!groupConnectionKeyMap.containsKey(group)) {
            initGroup(group);
        }
        List<String> connectionKey = groupConnectionKeyMap.get(group);
        if (CollectionUtils.isEmpty(connectionKey)) {
            return Lists.newArrayList();
        }
        List<ConnectionInstance> result = Lists.newArrayList();
        connectionKey.stream().forEach(s -> {
            ConnectionProxy connectionProxy = connectionInstanceMap.get(s);
            if (connectionProxy != null) {
                result.add(connectionProxy);
            }
        });
        return result;
    }

    /**
     * init one group
     *
     * @param group
     */
    private void initGroup(String group) {
        reentrantLock.lock();
        try {
            if (groupConnectionKeyMap.containsKey(group)) {
                return;
            }
            String code = scheduleServerConfig.getRegister().getCode();
            ServerRegister serverRegister = serverRegisterService.serverRegister(code);
            List<String> serverKeyList = Lists.newArrayList();
            List<ServerInstance> groupServerList = serverRegister.getAllServer(group);
            if (CollectionUtils.isEmpty(groupServerList)) {
                logger.warn("there is no active server in group [{}] ", group);
                return;
            }
            List<String> collect = groupServerList.stream().map(serverInstance -> getServerKey(serverInstance)).collect(Collectors.toList());

            //创建连接
            collect.stream().forEach(serverKey -> {
                if (!serverKeyList.contains(serverKey)) {
                    serverKeyList.add(serverKey);
                }
                if (!lazyInitConnect) {
                    initConnection(serverKey);
                }
            });
            logger.info("group [{}] has [{}] server instance ", group, JSON.toJSONString(serverKeyList));
            groupConnectionKeyMap.put(group, serverKeyList);

            serverRegister.addServerChangeListener(group, new ServerStateChangeListener() {
                @Override
                public void serverRemoved(ServerInstance serverInstance) {
                    logger.info("group server {} instance removed ,add new instance [{}] ", group, JSON.toJSONString(serverInstance));
                    groupConnectionKeyMap.get(group).remove(getServerKey(serverInstance));
                    doRemoveConnection(getServerKey(serverInstance));
                }

                @Override
                public void serverAdd(ServerInstance serverInstance) {
                    logger.info("group server {} instance add ,remove instance [{}] ", group, JSON.toJSONString(serverInstance));
                    addServerInstance(group, getServerKey(serverInstance));
                }

                @Override
                public void refreshAll(List<ServerInstance> serverInstanceList) {
                    logger.info("group server {} instance refresh,all server instance [{}] ", group, JSON.toJSONString(serverInstanceList));
                    doRefreshInstance(group, serverInstanceList);
                }
            });

        } catch (Throwable e) {
            logger.error("add connection group [" + group + "] monitor error ", e);
        } finally {
            reentrantLock.unlock();
        }

    }

    /**
     * refresh all the connection instance
     * <p>
     * lazy init the connection
     *
     * @param groupId
     * @param serverInstanceList
     */
    private void doRefreshInstance(String groupId, List<ServerInstance> serverInstanceList) {
        reentrantLock.lock();
        try {
            //build exist server
            Map<String, ServerInstance> newServerInstanceMap = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(serverInstanceList)) {
                for (ServerInstance serverInstance : serverInstanceList) {
                    String serverKey = getServerKey(serverInstance);
                    newServerInstanceMap.put(serverKey, serverInstance);
                }
            }
            //first remove
            List<String> connectionList = groupConnectionKeyMap.get(groupId);
            Iterator<String> iterator = connectionList.iterator();
            while (iterator.hasNext()) {
                String serverKey = iterator.next();
                if (!newServerInstanceMap.containsKey(serverKey)) {
                    doRemoveConnection(serverKey);
                    iterator.remove();
                }
            }
            //then add
            newServerInstanceMap.keySet().stream().forEach(newServer -> {
                if (!connectionList.contains(newServer)) {
                    connectionList.add(newServer);
                    if (!lazyInitConnect) {
                        initConnection(newServer);
                    }
                }
            });
        } catch (Throwable e) {
            logger.error("refresh instance exception " + groupId, e);
        } finally {
            reentrantLock.unlock();
        }
        logger.info("group [{}] refresh instance remain [{}]", groupId, connectionInstanceMap.size());
    }

    /**
     * get server key
     *
     * @param serverInstance
     * @return
     */
    private String getServerKey(ServerInstance serverInstance) {
        return String.format("%s_%s", serverInstance.getServer(), serverInstance.getPort());
    }

    /**
     * 关闭连接
     *
     * @param serverKey
     */
    private void doRemoveConnection(String serverKey) {
        reentrantLock.lock();
        try {
            ConnectionInstance connectionInstance = connectionInstanceMap.get(serverKey);
            if (connectionInstance != null) {
                connectionInstance.stateTrans(ConnectionState.REMOVED);
                boolean b = connectionInstance.disConnection();
                connectionInstanceMap.remove(serverKey);
                logger.info("remove instance [{}] result [{}]", serverKey, b);
            }
        } catch (Throwable e) {
            logger.error("remove connection exception " + serverKey, e);
        } finally {
            reentrantLock.unlock();
        }

    }

    /**
     * init one connection
     *
     * @param group
     * @param serverKey
     */
    private void addServerInstance(String group, String serverKey) {
        reentrantLock.lock();
        try {
            if (!groupConnectionKeyMap.containsKey(group)) {
                groupConnectionKeyMap.put(group, Lists.newArrayList());
            }
            groupConnectionKeyMap.get(group).add(serverKey);
            if (!lazyInitConnect) {
                initConnection(serverKey);
            }
        } catch (Throwable e) {
            logger.error("remove connection exception " + serverKey, e);
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * init connection
     *
     * @param serverKey
     * @param
     * @return
     */
    private ConnectionInstance initConnection(String serverKey) {
        reentrantLock.lock();
        try {
            ServerTransport serverTransport = serverTransportService
                    .serverTransport(scheduleServerConfig.getTransport().getCode());

            if (connectionInstanceMap.containsKey(serverKey)) {
                return connectionInstanceMap.get(serverKey);
            }
            String[] s = serverKey.split("_");
            ConnectionInstance connectionInstance = serverTransport.connectServer(s[0], Integer.parseInt(s[1]));
            logger.info("init connection to server [{}] result [{}]", serverKey, connectionInstanceMap != null);
            if (connectionInstance != null) {
                connectionInstanceMap.put(serverKey, ConnectionProxy.proxy(connectionInstance));
            }
            return connectionInstanceMap.get(serverKey);
        } catch (Throwable e) {
            logger.info("init connection to server [" + serverKey + "] exception", e);
        } finally {
            reentrantLock.unlock();
        }
        return null;
    }
}
