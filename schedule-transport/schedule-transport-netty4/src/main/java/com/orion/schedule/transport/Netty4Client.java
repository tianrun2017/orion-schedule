package com.orion.schedule.transport;

import com.orion.schedule.client.ConnectionInstance;
import com.orion.schedule.codec.ScheduleCodec;
import com.orion.schedule.config.transport.Netty4Config;
import com.orion.schedule.enums.ConnectionState;
import com.orion.schedule.util.Netty4Cleaner;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 15:39
 * @Version 1.0.0
 */
public class Netty4Client {
    Bootstrap bootstrap = null;
    EventLoopGroup eventExecutor;
    private Logger logger = LoggerFactory.getLogger(Netty4Client.class);
    private ScheduleCodec scheduleCodec;
    private Netty4Config netty4Config;

    private Netty4Client() {
    }

    public static Netty4Client getInstance() {
        return new Netty4Client();
    }

    public Netty4Client withCodec(ScheduleCodec scheduleCodec) {
        this.scheduleCodec = scheduleCodec;
        return this;
    }

    public Netty4Client withNett4Config(Netty4Config nett4Config) {
        this.netty4Config = nett4Config;
        return this;
    }

    /**
     * get a connetion instance
     *
     * @param remoteServer
     * @param port
     * @return
     * @throws Exception
     */
    public ConnectionInstance connect(String remoteServer, int port) {
        Netty4ConnectionInstance connectionInstance = new Netty4ConnectionInstance();
        connectionInstance.setRemoteServer(remoteServer);
        connectionInstance.setPort(port);
        /**
         * build connect instance
         */
        try {
            connectionInstance.stateTrans(ConnectionState.CONNECTING);
            Channel channel = doConnect(remoteServer, port, new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (!channelFuture.isSuccess()) {
                        if (connectionInstance.getConnectionState() == ConnectionState.REMOVED) {
                            logger.info("the server has been removed ,didn't try to reconnect [{}] [{}] ", remoteServer, port);
                            channelFuture.channel().close();
                            return;
                        }
                        logger.warn("reconnect ");
                        connectionInstance.stateTrans(ConnectionState.RECONNECTING);
                        EventLoop eventExecutors = channelFuture.channel().eventLoop();
                        eventExecutors.schedule(() -> {
                            try {
                                Channel newChannel = doConnect(remoteServer, port, this);
                                connectionInstance.stateTrans(ConnectionState.SUCCESS);
                                connectionInstance.setChannel(newChannel);
                            } catch (Throwable e) {
                                logger.error("reconnect error ", e);
                                connectionInstance.stateTrans(ConnectionState.FAIL);
                            }
                        }, 1, TimeUnit.SECONDS);
                    }
                }
            });
            connectionInstance.setChannel(channel);
            return connectionInstance;
        } catch (Throwable e) {
            connectionInstance.stateTrans(ConnectionState.FAIL);
            logger.error(String.format("connect to server [%s],port [%s] exception", remoteServer, port), e);
        }
        return connectionInstance;
    }

    /**
     * do a connection
     *
     * @param server
     * @param port
     * @param connectionListener
     * @return
     * @throws Exception
     */
    public Channel doConnect(String server, int port, ChannelFutureListener connectionListener) throws Exception {
        Channel channel = bootstrap.connect(server, port).addListener(connectionListener).sync().channel();
        return channel;
    }

    /**
     * init a connection
     *
     * @return
     */
    public void init() {
        bootstrap = new Bootstrap();
        eventExecutor = new NioEventLoopGroup(10,
                new DefaultThreadFactory("scheduleClientWorker", true));
        try {
            bootstrap.group(eventExecutor).channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, netty4Config.getConnectionTimeOut())
                    .handler(new ClientChannelHandler(scheduleCodec, netty4Config.getIdleTime()));
            addShutdownHolder();
        } catch (Throwable e) {
            logger.error("build client error ", e);
            throw e;
        }
    }

    private void addShutdownHolder() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("shutdown client ");
            if (eventExecutor != null) {
                eventExecutor.shutdownGracefully();
            }
            Netty4Cleaner.clean(netty4Config.getCloseWait());
        }));
    }

}
