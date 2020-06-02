package com.orion.schedule.register;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.orion.schedule.common.util.InetUtils;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.config.register.EtcdRegister;
import com.orion.schedule.register.listener.ServerStateChangeListener;
import com.orion.schedule.transport.ServerTransport;
import com.orion.schedule.transport.ServerTransportService;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import io.grpc.stub.StreamObserver;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author sevenzhong
 * @Date 2019/10/10 15:32
 */
public class EtcdServerRegister implements ServerRegister {
    private Logger logger = LoggerFactory.getLogger(EtcdServerRegister.class);

    private static String serviceNamePrefix = "/schedule_register/%s/";
    private static String endPrefix = "/schedule_register/%s/2";

    @Autowired
    private ScheduleServerConfig scheduleServerConfig;

    @Autowired
    private ServerTransportService serverTransportService;

    private Charset charset = Charset.forName("utf-8");
    private Client client;
    private int ttl;
    private long leaseId;
    private ByteSequence ZEROVALUE = ByteSequence.from(new byte[]{0});

    public void init() throws Exception {
        EtcdRegister config = scheduleServerConfig.getRegister().getEtcdConfig();
        if (CollectionUtils.isEmpty(config.getServerList())) {
            throw new RuntimeException("etcd config must contains serverList config");
        }
        ttl = scheduleServerConfig.getRegister().getEtcdConfig().getTtl();
        List<URI> serverURLList = config.getServerList().stream().map(serverUrl -> URI.create(serverUrl)).collect(Collectors.toList());
        client = Client.builder().endpoints(serverURLList).build();
    }

    @Override
    public boolean register() throws Exception {
        try {
            Set<String> set = Sets.newHashSet();
            scheduleServerConfig.getTask().getGroupList().stream().forEach(group -> {
                if (StringUtils.isNotEmpty(group)) {
                    set.add(group);
                }
            });
            ServerTransport transport = serverTransportService.serverTransport(scheduleServerConfig.getTransport().getCode());
            String selfIp = InetUtils.getSelfIp();
            LeaseGrantResponse leaseGrantResponse = client.getLeaseClient().grant(ttl).get();
            leaseId = leaseGrantResponse.getID();
            client.getLeaseClient().keepAlive(leaseId, new StreamObserver<LeaseKeepAliveResponse>() {
                @Override
                public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {
                    logger.error("keepAlive invoke");
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.error("keepAlive onError invoke", throwable);
                }

                @Override
                public void onCompleted() {
                    logger.error("keepAlive onCompleted invoke");
                }
            });
            logger.warn("lease id is {} ", leaseId);
            if (leaseId <= 0) {
                throw new RuntimeException("init lease exception ,the lease id small than zero");
            }
            PutOption putOption = PutOption.newBuilder().withLeaseId(leaseId).build();
            for (String group : set) {
                String serviceName = String.format(serviceNamePrefix, group) + selfIp;
                client.getKVClient().put(parseToSequence(serviceName), parseToSequence(transport.transportPort() + ""), putOption);
                logger.info("Register a new service at:{}", serviceName);
            }
            return true;
        } catch (Throwable e) {
            logger.error("etcd Server not available.");
            throw e;
        }
    }

    @Override
    public boolean unRegister() throws Exception {
        Set<String> set = Sets.newHashSet();
        scheduleServerConfig.getTask().getGroupList().stream().forEach(group -> {
            if (StringUtils.isNotEmpty(group)) {
                set.add(group);
            }
        });
        String selfIp = InetUtils.getSelfIp();
        for (String group : set) {
            String serviceName = String.format(serviceNamePrefix, group) + selfIp;
            client.getKVClient().delete(parseToSequence(serviceName));
            logger.info("server unRegister success [{}] ", serviceName);
        }
        return true;
    }

    @Override
    public List<ServerInstance> getAllServer(String groupId) throws Exception {
        String serviceName = String.format(serviceNamePrefix, groupId);
        GetOption getOption = GetOption.newBuilder().withPrefix(parseToSequence(String.format(endPrefix, groupId))).build();
        GetResponse getResponse = client.getKVClient().get(parseToSequence(serviceName), getOption).get();
        long aliveCount = getResponse.getCount();
        logger.info("available count is {} ", aliveCount);
        if (aliveCount > 0) {
            List<KeyValue> kvs = getResponse.getKvs();
            List<ServerInstance> collect = kvs.stream().map(keyValue -> buildServerInstance(keyValue)).filter(serverInstance -> serverInstance != null).collect(Collectors.toList());
            return collect;
        }
        return Lists.newArrayList();
    }

    @Override
    public void addServerChangeListener(String groupId, ServerStateChangeListener serverStateChangeListener) throws Exception {
        logger.info("register listener");
        WatchOption watchOption = WatchOption.newBuilder().withPrefix(parseToSequence(String.format(endPrefix, groupId))).build();
        client.getWatchClient().watch(parseToSequence(String.format(serviceNamePrefix, groupId)), watchOption, new Watch.Listener() {
            @Override
            public void onNext(WatchResponse response) {
                List<WatchEvent> events = response.getEvents();
                if (CollectionUtils.isNotEmpty(events)) {
                    events.stream().forEach(watchEvent -> {
                        if (watchEvent.getEventType() == WatchEvent.EventType.PUT) {
                            KeyValue keyValue = watchEvent.getKeyValue();
                            logger.info("add server {} ", parseSequenceToStr(keyValue.getKey()));
                            ServerInstance serverInstance = buildServerInstance(keyValue);
                            if (serverInstance != null) {
                                serverStateChangeListener.serverAdd(serverInstance);
                            }
                            KeyValue prevKV = watchEvent.getPrevKV();
                            if (prevKV != null &&
                                    prevKV.getKey().getBytes().length > 0 && prevKV.getValue().getBytes().length > 0) {
                                serverInstance = buildServerInstance(prevKV);
                                if (serverInstance != null) {
                                    serverStateChangeListener.serverRemoved(serverInstance);
                                }
                            }
                        } else if (watchEvent.getEventType() == WatchEvent.EventType.DELETE) {
                            logger.info("delete server {} ", parseSequenceToStr(watchEvent.getKeyValue().getKey()));
                            ServerInstance serverInstance = buildServerInstance(watchEvent.getKeyValue());
                            if (serverInstance != null) {
                                serverStateChangeListener.serverRemoved(serverInstance);
                            }
                        }
                    });
                }

            }

            @Override
            public void onError(Throwable throwable) {
                logger.error(groupId + " etcd instance error", throwable);
            }

            @Override
            public void onCompleted() {
                logger.error(groupId + " etcd instance complete");
            }
        });
    }

    /**
     * build server instance
     *
     * @param keyValue
     * @return
     */
    private ServerInstance buildServerInstance(KeyValue keyValue) {
        ServerInstance serverInstance = new ServerInstance();
        String key = parseSequenceToStr(keyValue.getKey());
        String ip = key.substring(key.lastIndexOf("/") + 1);
        serverInstance.setServer(ip);
        if (keyValue.getValue().getBytes().length > 0) {
            String s = parseSequenceToStr(keyValue.getValue());
            serverInstance.setPort(Integer.parseInt(s));
        }
        return serverInstance;
    }

    @Override
    public String registerCode() {
        return "etcd";
    }


    private ByteSequence parseToSequence(String str) {
        return ByteSequence.from(str, charset);
    }

    private String parseSequenceToStr(ByteSequence byteSequence) {
        return byteSequence.toString(charset);
    }
}
