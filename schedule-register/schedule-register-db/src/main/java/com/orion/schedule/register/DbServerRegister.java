package com.orion.schedule.register;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.orion.schedule.common.util.InetUtils;
import com.orion.schedule.config.DataSourceConfig;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.progress.util.ScheduleEncrypt;
import com.orion.schedule.register.listener.ServerStateChangeListener;
import com.orion.schedule.transport.ServerTransport;
import com.orion.schedule.transport.ServerTransportService;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @Description
 * @Author sevenzhong
 * @Date 2019/10/10 15:32
 */
public class DbServerRegister implements ServerRegister {
    private Logger logger = LoggerFactory.getLogger(DbServerRegister.class);

    private DruidDataSource druidDataSource;

    @Autowired
    private ScheduleServerConfig scheduleServerConfig;

    @Autowired
    private ServerTransportService serverTransportService;

    private ScheduledExecutorService scheduledExecutorService;

    private Map<String, Set<String>> serverMap = Maps.newHashMap();

    public void init() {
        buildDataSource();
        scheduledExecutorService = new ScheduledThreadPoolExecutor(5,
                new BasicThreadFactory.Builder().namingPattern("alive-server-%d").priority(10).daemon(true).build());
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Iterator<String> iterator = serverMap.keySet().iterator();
                while (iterator.hasNext()) {
                    String group = iterator.next();
                    Set<String> serverKeySet = serverMap.get(group);
                    if (CollectionUtils.isNotEmpty(serverKeySet)) {
                        serverKeySet.stream().forEach(serverKey -> {
                            String[] split = serverKey.split(":");
                            updateAlive(group, Integer.parseInt(split[1]), split[0]);
                        });
                    }
                }
            }
        }, 1, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * build DataSource
     *
     * @return
     * @throws Exception
     */
    @SneakyThrows
    public DruidDataSource buildDataSource() {
        DataSourceConfig dataSource = scheduleServerConfig.getRegister().getDbConfig();
        druidDataSource = new DruidDataSource();
        if (StringUtils.isNotEmpty(dataSource.getToken())) {
            String decrypt = ScheduleEncrypt.decrypt(dataSource.getToken());
            JSONObject jsonObject = JSON.parseObject(decrypt);
            druidDataSource.setUrl(jsonObject.getString("url"));
            druidDataSource.setUsername(jsonObject.getString("user"));
            druidDataSource.setPassword(jsonObject.getString("pwd"));
        } else {
            druidDataSource.setUsername(ScheduleEncrypt.decrypt(dataSource.getUserName()));
            druidDataSource.setPassword(ScheduleEncrypt.decrypt(dataSource.getPassword()));
            druidDataSource.setUrl(ScheduleEncrypt.decrypt(dataSource.getUrl()));
        }
        druidDataSource.setMaxActive(5);
        druidDataSource.setDriverClassName(dataSource.getDriverClassName());
        druidDataSource.setDefaultAutoCommit(false);
        druidDataSource.init();
        return druidDataSource;
    }

    @Override
    public boolean register() throws Exception {
        try {
            Set<String> set = Sets.newHashSet();
            scheduleServerConfig.getTask().getGroupList().stream().forEach(group -> {
                if (StringUtils.isNotEmpty(group)) {
                    set.add(group);
                    serverMap.put(group, new HashSet<>());
                }
            });
            ServerTransport transport = serverTransportService.serverTransport(scheduleServerConfig.getTransport().getCode());
            String selfIp = InetUtils.getSelfIp();
            for (String group : set) {
                boolean b = addOrUpdateInstance(group, transport.transportPort(), selfIp);
                logger.info("Register a new service at:{} {} {},result is {}", group, transport.transportPort(), selfIp, b);
                serverMap.get(group).add(buildServerKey(selfIp, transport.transportPort()));
            }
            return true;
        } catch (Throwable e) {
            logger.error("db register fail .", e);
            throw e;
        }
    }

    /**
     * add instance
     *
     * @param group
     * @param transportPort
     * @param selfIp
     * @return
     */
    private boolean addOrUpdateInstance(String group, int transportPort, String selfIp) {
        PreparedStatement preparedStatement = null;
        DruidPooledConnection connection = null;
        ResultSet resultSet = null;
        List<ServerInstance> result = Lists.newArrayList();
        try {
            connection = druidDataSource.getConnection();
            preparedStatement = connection.prepareStatement("select count(1) as nu from service_alive_record where group_id =? and ip=? and port=? ");
            preparedStatement.setString(1, group);
            preparedStatement.setString(2, selfIp);
            preparedStatement.setLong(3, transportPort);
            resultSet = preparedStatement.executeQuery();
            boolean next = resultSet.next();
            int nu = next ? resultSet.getInt("nu") : 0;
            resultSet.close();
            preparedStatement.close();
            if (nu > 0) {
                return true;
            } else {
                preparedStatement = connection.prepareStatement("insert into service_alive_record(group_id,ip,port) values(?,?,?)");
                preparedStatement.setString(1, group);
                preparedStatement.setString(2, selfIp);
                preparedStatement.setInt(3, transportPort);
                int i = preparedStatement.executeUpdate();
                preparedStatement.close();
                connection.commit();
                if (i > 0) {
                    logger.warn("add new service instance {} {} {} ", group, selfIp, transportPort);
                }
            }
        } catch (Throwable e) {
        } finally {
            closeQuite(resultSet, preparedStatement, connection);
        }
        return true;
    }

    /**
     * update ttl
     *
     * @param group
     * @param transportPort
     * @param selfIp
     */
    private void updateAlive(String group, int transportPort, String selfIp) {
        PreparedStatement preparedStatement = null;
        DruidPooledConnection connection = null;
        try {
            connection = druidDataSource.getConnection();
            preparedStatement = connection.prepareStatement("update service_alive_record set ttl_update_time =? ,status=0 where group_id=? and ip=? and port=?");
            preparedStatement.setLong(1, System.currentTimeMillis());
            preparedStatement.setString(2, group);
            preparedStatement.setString(3, selfIp);
            preparedStatement.setInt(4, transportPort);
            int i = preparedStatement.executeUpdate();
            connection.commit();
        } catch (Throwable e) {
            logger.error("update alive error ", e);
        } finally {
            closeQuite(preparedStatement, connection);
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
        ServerTransport transport = serverTransportService.serverTransport(scheduleServerConfig.getTransport().getCode());
        String selfIp = InetUtils.getSelfIp();
        for (String group : set) {
            Set<String> strings = serverMap.get(group);
            if (strings != null) {
                String serverKey = buildServerKey(selfIp, transport.transportPort());
                strings.remove(serverKey);
                boolean b = invalidateInstance(group, transport.transportPort(), selfIp);
                logger.info("Register a new service at:{} {} {},result is {}", group, transport.transportPort(), selfIp, b);
            }
        }
        return true;
    }

    /**
     * add new instance
     *
     * @param group
     * @param transportPort
     * @param selfIp
     * @return
     */
    private boolean invalidateInstance(String group, int transportPort, String selfIp) {
        PreparedStatement preparedStatement = null;
        DruidPooledConnection connection = null;
        try {
            connection = druidDataSource.getConnection();
            preparedStatement = connection.prepareStatement("update service_alive_record set status=1 where group_id=? and ip=? and port=?");
            preparedStatement.setString(1, group);
            preparedStatement.setString(2, selfIp);
            preparedStatement.setInt(3, transportPort);
            int i = preparedStatement.executeUpdate();
            connection.commit();
            logger.info("invalidate server instance {} {} {} result is {} ", group, selfIp, transportPort, i > 0);
        } catch (Throwable e) {
            logger.error("invalidate alive error ", e);
        } finally {
            closeQuite(preparedStatement, connection);
        }
        return true;
    }

    @Override
    public List<ServerInstance> getAllServer(String groupId) throws Exception {
        PreparedStatement preparedStatement = null;
        DruidPooledConnection connection = null;
        ResultSet resultSet = null;
        List<ServerInstance> result = Lists.newArrayList();
        try {
            connection = druidDataSource.getConnection();
            Long now = System.currentTimeMillis();
            preparedStatement = connection.prepareStatement("select ip,port from service_alive_record where group_id =? and status=0 and ttl_update_time +30 > ?");
            preparedStatement.setString(1, groupId);
            preparedStatement.setLong(2, now);
            resultSet = preparedStatement.executeQuery();
            if (!serverMap.containsKey(groupId)) {
                serverMap.put(groupId, new HashSet<>());
            }
            while (resultSet.next()) {
                String ip = resultSet.getString("ip");
                int port = resultSet.getInt("port");
                serverMap.get(groupId).add(buildServerKey(ip, port));
                ServerInstance serverInstance = ServerInstance.defaultInstance().withServer(ip).withPort(port);
                result.add(serverInstance);
            }
        } finally {
            closeQuite(resultSet, preparedStatement, connection);
        }
        return result;
    }

    /**
     * build server key
     *
     * @param selfIp
     * @param port
     * @return
     */
    private String buildServerKey(String selfIp, int port) {
        return String.format("%s:%s", selfIp, port);
    }

    @Override
    public void addServerChangeListener(String groupId, ServerStateChangeListener serverStateChangeListener) throws Exception {
        logger.info("register listener");
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            PreparedStatement preparedStatement = null;
            DruidPooledConnection connection = null;
            ResultSet resultSet = null;
            try {
                connection = druidDataSource.getConnection();
                Long now = System.currentTimeMillis();
                preparedStatement = connection.prepareStatement("select ip,port,ttl_update_time from service_alive_record where group_id =? and ttl_update_time + 1000*60*5 > ?");
                preparedStatement.setString(1, groupId);
                preparedStatement.setLong(2, now);
                resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    String ip = resultSet.getString("ip");
                    int port = resultSet.getInt("port");
                    Long ttl = resultSet.getLong("ttl_update_time");
                    String serverKey = buildServerKey(ip, port);
                    long l = ttl - now;
                    if (l > 1000 * 2) {
                        //not active
                        Set<String> groupInstanceSet = serverMap.get(groupId);
                        if (groupInstanceSet != null && groupInstanceSet.contains(serverKey)) {
                            ServerInstance serverInstance = ServerInstance.defaultInstance().withPort(port).withServer(ip);
                            serverStateChangeListener.serverRemoved(serverInstance);
                            groupInstanceSet.remove(serverKey);
                        }
                    } else {
                        //active or new instance
                        Set<String> groupInstanceSet = serverMap.get(groupId);
                        if (groupInstanceSet != null && !groupInstanceSet.contains(serverKey)) {
                            //has new instance join
                            ServerInstance serverInstance = ServerInstance.defaultInstance().withPort(port).withServer(ip);
                            serverStateChangeListener.serverAdd(serverInstance);
                            groupInstanceSet.add(serverKey);
                        }
                    }
                }
            } catch (Throwable e) {
                logger.error("load service info exception ", e);
            } finally {
                closeQuite(resultSet, preparedStatement, connection);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * close quite
     *
     * @param closeable
     */
    private void closeQuite(AutoCloseable... closeable) {
        for (AutoCloseable closeable1 : closeable) {
            try {
                if (closeable1 != null) {
                    closeable1.close();
                }
            } catch (Throwable e) {
                logger.error("close connection exception", e);
            }
        }
    }

    @Override
    public String registerCode() {
        return "db";
    }
}
