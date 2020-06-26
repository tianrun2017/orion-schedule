package com.orion.schedule.register;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.orion.schedule.common.util.InetUtils;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.enums.RegisterType;
import com.orion.schedule.register.listener.ServerStateChangeListener;
import com.orion.schedule.transport.ServerTransport;
import com.orion.schedule.transport.ServerTransportService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/3 16:15
 * @Version 1.0.0
 */
public class ZookeeperServerRegister implements ServerRegister {

    private static String serviceNamePrefix = "/orion/schedule/virtual/%s/%s:%s";
    ZooKeeper zk = null;
    private Logger logger = LoggerFactory.getLogger(ZookeeperServerRegister.class);
    private static byte[] data = new byte[]{(byte) 0};
    List<ACL> aclList = ZooDefs.Ids.OPEN_ACL_UNSAFE;
    @Autowired
    private ScheduleServerConfig scheduleServerConfig;
    @Autowired
    private ServerTransportService serverTransportService;

    public void init() {
        List<String> serverList = scheduleServerConfig.getRegister().getZkConfig().getServerList();
        String join = StringUtils.join(serverList, ",");
        try {
            zk = new ZooKeeper(join, scheduleServerConfig.getRegister().getZkConfig().getTimeout(), new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    logger.info("event {} ", JSON.toJSONString(event));
                }
            });

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean register() throws Exception {
        Set<String> set = Sets.newHashSet();
        scheduleServerConfig.getTask().getGroupList().stream().forEach(group -> {
            if (StringUtils.isNotEmpty(group)) {
                set.add(group);
            }
        });
        String selfIp = InetUtils.getSelfIp();
        for (String group : set) {
            String groupPath = String.format("/orion/schedule/virtual/%s", group);
            createNodeRecursive(groupPath, CreateMode.PERSISTENT);
            ServerTransport serverTransport = serverTransportService.serverTransport(scheduleServerConfig.getTransport().getCode());
            String serviceName = String.format(serviceNamePrefix, group, selfIp, serverTransport.transportPort());
            zk.create(serviceName, data, aclList,CreateMode.EPHEMERAL);
            logger.info("server register success [{}] ", serviceName);
        }
        return true;
    }


    @Override
    public boolean unRegister() throws Exception {
        Set<String> set = Sets.newHashSet();
        scheduleServerConfig.getTask().getGroupList().stream().forEach(group -> {
            if (StringUtils.isNotEmpty(group)) {
                set.add(group);
            }
        });
        ServerTransport serverTransport = serverTransportService.serverTransport(scheduleServerConfig.getTransport().getCode());
        String selfIp = InetUtils.getSelfIp();
        for (String group : set) {
            String serviceName = String.format(serviceNamePrefix, group, selfIp, serverTransport.transportPort());
            zk.delete(serviceName, -1);
            logger.info("server unRegister success [{}] ", serviceName);
        }
        return true;
    }

    @Override
    public List<ServerInstance> getAllServer(String groupId) throws Exception {
        String serviceNamePrefix = String.format("/orion/schedule/virtual/%s", groupId);
        List<String> allInstances = zk.getChildren(serviceNamePrefix, null);
        if (CollectionUtils.isNotEmpty(allInstances)) {
            List<ServerInstance> collect = allInstances.stream().map(instance -> {
                String[] split = instance.split(":");
                return ServerInstance.defaultInstance().withPort(Integer.parseInt(split[1])).withServer(split[0]);
            }).collect(Collectors.toList());
            return collect;
        }
        return Lists.newArrayList();
    }

    @Override
    public void addServerChangeListener(String groupId, ServerStateChangeListener serverStateChangeListener) throws Exception {
        String serviceName = String.format("/orion/schedule/virtual/%s", groupId);
        zk.exists(serviceName, event -> {
            if (event.getType() == Watcher.Event.EventType.NodeCreated) {
                String path = event.getPath();
                String substring = path.substring(path.lastIndexOf("/"));
                String[] split = substring.split(":");
                ServerInstance serverInstance = ServerInstance.defaultInstance().withPort(Integer.parseInt(split[1])).withServer(split[0]);
                serverStateChangeListener.serverAdd(serverInstance);
            } else if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
                String path = event.getPath();
                String substring = path.substring(path.lastIndexOf("/"));
                String[] split = substring.split(":");
                ServerInstance serverInstance = ServerInstance.defaultInstance().withPort(Integer.parseInt(split[1])).withServer(split[0]);
                serverStateChangeListener.serverRemoved(serverInstance);
            }
        });
    }

    /**
     * create node
     *
     * @param path
     * @throws Exception
     */
    private void createNodeRecursive(String path, CreateMode createMode) throws Exception {
        if (path.lastIndexOf("/") == 0) {
          String s = zk.create(path, data, aclList, createMode);
          logger.info("create result 1 is {} {} ",path,s);
        } else {
            Stat exists = zk.exists(path, null);
            if (exists != null) {
                logger.info("path exist {} ",path);
                return;
            }
            String substring = path.substring(0, path.lastIndexOf("/"));
            exists = zk.exists(substring, null);
            if (exists != null) {
                String s = zk.create(path, data, aclList, createMode);
                logger.info("create result 2 is {} {} ",path,s);
            } else {
                logger.info("path not exist {} ",substring);
                createNodeRecursive(substring, createMode);
                String s = zk.create(path, data, aclList, createMode);
                logger.info("create result 3 is {} {} ",path,s);
            }
        }
    }

    @Override
    public String registerCode() {
        return RegisterType.ZK.getCode();
    }
}
