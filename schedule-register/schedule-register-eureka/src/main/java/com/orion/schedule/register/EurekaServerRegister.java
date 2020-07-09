package com.orion.schedule.register;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import com.orion.schedule.common.util.InetUtils;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.enums.RegisterType;
import com.orion.schedule.register.listener.ServerStateChangeListener;
import com.orion.schedule.transport.ServerTransport;
import com.orion.schedule.transport.ServerTransportService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/3 16:15
 * @Version 1.0.0
 */
public class EurekaServerRegister implements ServerRegister {

    private static String serviceNamePrefix = "com.orion.schedule_server_group:%s";
    private Logger logger = LoggerFactory.getLogger(EurekaServerRegister.class);
    @Autowired
    private ScheduleServerConfig scheduleServerConfig;
    @Autowired
    private ServerTransportService serverTransportService;

    private DefaultEurekaClientConfig defaultEurekaClientConfig = null;

    Map<String, DiscoveryClient> discoveryClientMap = Maps.newHashMap();

    public void init() {
        defaultEurekaClientConfig = new DefaultEurekaClientConfig() {
            @Override
            public List<String> getEurekaServerServiceUrls(String myZone) {
                return scheduleServerConfig.getRegister().getServerList();
            }


            @Override
            public boolean shouldDisableDelta() {
                return false;
            }

            @Override
            public boolean shouldRegisterWithEureka() {
                return scheduleServerConfig.getRegister().getEurekaConfig().isRegisterSelf();
            }

            @Override
            public int getRegistryFetchIntervalSeconds() {
                return 1;
            }

        };
    }

    @Override
    public boolean register() throws Exception {
        Set<String> set = Sets.newHashSet();
        scheduleServerConfig.getTask().getGroupList().stream().forEach(group -> {
            if (StringUtils.isNotEmpty(group)) {
                set.add(group);
            }
        });
        ServerTransport serverTransport = serverTransportService.serverTransport(scheduleServerConfig.getTransport().getCode());
        String selfIp = InetUtils.getSelfIp();
        for (String group : set) {
            String serviceName = String.format(serviceNamePrefix, group);
            ScheduleEurekaInstance scheduleEurekaInstance = new ScheduleEurekaInstance(serviceName, selfIp, serverTransport.transportPort());
            ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(scheduleEurekaInstance, (ApplicationInfoManager.OptionalArgs) null);
            DiscoveryClient discoveryClient = new DiscoveryClient(applicationInfoManager, defaultEurekaClientConfig);
            discoveryClientMap.put(serviceName, discoveryClient);
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
        for (String group : set) {
            String serviceName = String.format(serviceNamePrefix, group);
            DiscoveryClient remove = discoveryClientMap.remove(serviceName);
            if (remove != null) {
                remove.shutdown();
            }
            logger.info("server unRegister success [{}] ", serviceName);
        }
        return true;
    }

    @Override
    public List<ServerInstance> getAllServer(String groupId) throws Exception {
        String serviceName = String.format(serviceNamePrefix, groupId);
        DiscoveryClient discoveryClient = discoveryClientMap.get(serviceName);
        if (discoveryClient == null) {
            synchronized (EurekaServerRegister.class) {
                discoveryClient = discoveryClientMap.get(serviceName);
                if (discoveryClient == null) {
                    ScheduleEurekaInstance scheduleEurekaInstance = new ScheduleEurekaInstance(serviceName, "", -1);
                    ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(scheduleEurekaInstance, (ApplicationInfoManager.OptionalArgs) null);
                    discoveryClient = new DiscoveryClient(applicationInfoManager, defaultEurekaClientConfig);
                    discoveryClientMap.put(serviceName, discoveryClient);
                }
            }
        }
        Application application = discoveryClient.getApplication(serviceName);
        if (application != null) {
            List<InstanceInfo> instances = application.getInstances();
            if (CollectionUtils.isNotEmpty(instances)) {
                List<ServerInstance> collect = instances.stream().map(instanceInfo -> {
                    String instanceId = instanceInfo.getInstanceId();
                    String[] split = instanceId.split(":");
                    ServerInstance serverInstance = ServerInstance.defaultInstance().withServer(split[0]).withPort(Integer.parseInt(split[1]));
                    return serverInstance;
                }).collect(Collectors.toList());
                return collect;
            }
        }
        return Lists.newArrayList();
    }

    @Override
    public void addServerChangeListener(String groupId, ServerStateChangeListener serverStateChangeListener) throws Exception {
        String serviceName = String.format(serviceNamePrefix, groupId);
        DiscoveryClient discoveryClient = discoveryClientMap.get(serviceName);
        if (discoveryClient != null) {
            Application application = discoveryClient.getApplication(serviceName);
            List<InstanceInfo> instances = application.getInstancesAsIsFromEureka();
            List<ServerInstance> serverInstanceList = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(instances)) {
                instances.stream().forEach(instanceInfo -> {
                            String[] split = instanceInfo.getInstanceId().split(":");
                            ServerInstance serverInstance = ServerInstance.defaultInstance().withServer(split[0]).withPort(Integer.parseInt(split[1]));
                            serverInstanceList.add(serverInstance);
                        }
                );
            }
            serverStateChangeListener.refreshAll(serverInstanceList);
        }
    }

    @Override
    public String registerCode() {
        return RegisterType.EUREKA.getCode();
    }
}
