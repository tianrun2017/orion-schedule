//package com.orion.schedule.register;
//
//import com.google.common.collect.Lists;
//import com.google.common.collect.Sets;
//import com.netflix.appinfo.*;
//import com.orion.schedule.common.util.InetUtils;
//import com.orion.schedule.config.ScheduleServerConfig;
//import com.orion.schedule.config.register.NacosRegister;
//import com.orion.schedule.enums.RegisterType;
//import com.orion.schedule.register.listener.ServerStateChangeListener;
//import com.orion.schedule.transport.ServerTransport;
//import com.orion.schedule.transport.ServerTransportService;
//import org.apache.commons.collections4.CollectionUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.net.InetAddress;
//import java.util.List;
//import java.util.Properties;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
//import com.netflix.discovery.DefaultEurekaClientConfig;
//import com.netflix.discovery.DiscoveryClient;
//import com.netflix.discovery.EurekaClient;
//import com.netflix.discovery.EurekaClientConfig;
//
///**
// * @Description TODO
// * @Author beedoorwei
// * @Date 2019/6/3 16:15
// * @Version 1.0.0
// */
//public class EurekaServerRegister implements ServerRegister {
//
//    private static String serviceNamePrefix = "com.orion.schedule_server_group:%s";
//    DiscoveryClient discoveryClient;
//    private Logger logger = LoggerFactory.getLogger(EurekaServerRegister.class);
//    @Autowired
//    private ScheduleServerConfig scheduleServerConfig;
//    @Autowired
//    private ServerTransportService serverTransportService;
//
//    public void init() {
//        EurekaInstanceConfig eurekaInstanceConfig = new CloudInstanceConfig();
//        ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(eurekaInstanceConfig, (ApplicationInfoManager.OptionalArgs) null);
//        DefaultEurekaClientConfig defaultEurekaClientConfig = new DefaultEurekaClientConfig();
//        discoveryClient = new DiscoveryClient(applicationInfoManager,defaultEurekaClientConfig);
//    }
//
//    @Override
//    public boolean register() throws Exception {
//        Set<String> set = Sets.newHashSet();
//        scheduleServerConfig.getTask().getGroupList().stream().forEach(group -> {
//            if (StringUtils.isNotEmpty(group)) {
//                set.add(group);
//            }
//        });
//        ServerTransport serverTransport = serverTransportService.serverTransport(scheduleServerConfig.getTransport().getCode());
//        String selfIp = InetUtils.getSelfIp();
//        for (String group : set) {
//            String serviceName = String.format(serviceNamePrefix, group);
//            namingService.registerInstance(serviceName, scheduleServerConfig.getRegister().getRegisterEnv(), selfIp, serverTransport.transportPort());
//            logger.info("server register success [{}] ", serviceName);
//        }
//        return true;
//    }
//
//    @Override
//    public boolean unRegister() throws Exception {
//        Set<String> set = Sets.newHashSet();
//        scheduleServerConfig.getTask().getGroupList().stream().forEach(group -> {
//            if (StringUtils.isNotEmpty(group)) {
//                set.add(group);
//            }
//        });
//        ServerTransport serverTransport = serverTransportService.serverTransport(scheduleServerConfig.getTransport().getCode());
//        String selfIp = InetUtils.getSelfIp();
//        for (String group : set) {
//            String serviceName = String.format(serviceNamePrefix, group);
//            namingService.deregisterInstance(serviceName, scheduleServerConfig.getRegister().getRegisterEnv(), selfIp, serverTransport.transportPort());
//            logger.info("server unRegister success [{}] ", serviceName);
//        }
//        return true;
//    }
//
//    @Override
//    public List<ServerInstance> getAllServer(String groupId) throws Exception {
//        String serviceName = String.format(serviceNamePrefix, groupId);
//        List<Instance> allInstances = namingService.getAllInstances(serviceName, scheduleServerConfig.getRegister().getRegisterEnv());
//        if (CollectionUtils.isNotEmpty(allInstances)) {
//            List<ServerInstance> collect = allInstances.stream().map(instance -> ServerInstance.defaultInstance().withPort(instance.getPort())
//                    .withServer(instance.getIp())).collect(Collectors.toList());
//            return collect;
//        }
//        return Lists.newArrayList();
//    }
//
//    @Override
//    public void addServerChangeListener(String groupId, ServerStateChangeListener serverStateChangeListener) throws Exception {
//        String serviceName = String.format(serviceNamePrefix, groupId);
//        namingService.subscribe(serviceName, scheduleServerConfig.getRegister().getRegisterEnv(), event -> {
//            //refresh all the server instance
//            NamingEvent namingEvent = (NamingEvent) (event);
//            List<Instance> instances = namingEvent.getInstances();
//            List<ServerInstance> serverInstanceList = Lists.newArrayList();
//            if (CollectionUtils.isNotEmpty(instances)) {
//                serverInstanceList = instances.stream()
//                        .map(instance -> ServerInstance.defaultInstance().withServer(instance.getIp())
//                                .withPort(instance.getPort())).collect(Collectors.toList());
//
//            }
//            serverStateChangeListener.refreshAll(serverInstanceList);
//        });
//
//    }
//
//    @Override
//    public String registerCode() {
//        return RegisterType.NACOS.getCode();
//    }
//}
