package com.orion.schedule.transport;

import com.orion.schedule.codec.ScheduleCodec;
import com.orion.schedule.config.transport.Netty4Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 15:39
 * @Version 1.0.0
 */
public class Netty4Server {

    private static Logger logger = LoggerFactory.getLogger(Netty4Server.class);
    NioEventLoopGroup bossGroup;
    NioEventLoopGroup workerGroup;
    private ScheduleCodec scheduleCodec;
    private Netty4Config netty4Config;
    private Netty4Server() {
    }

    /**
     * @return
     */
    public static Netty4Server defaultServer() {
        return new Netty4Server();
    }

    public Netty4Server withCodec(ScheduleCodec scheduleCodec) {
        this.scheduleCodec = scheduleCodec;
        return this;
    }

    public Netty4Server withNetty4Config(Netty4Config netty4Config) {
        this.netty4Config = netty4Config;
        return this;
    }

    public void buildServer() throws Exception {
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("scheduleServerBoss", true));
        workerGroup = new NioEventLoopGroup(10,
                new DefaultThreadFactory("scheduleServerWorker", true));
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(NioChannelOption.SO_REUSEADDR, true)
                    .option(NioChannelOption.TCP_NODELAY, true)
                    .option(NioChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(NioChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ServerChannelHandler(scheduleCodec, netty4Config.getIdleTime()));
            Channel channel = serverBootstrap.bind(netty4Config.getServerPort()).sync().channel();
            logger.info("start schedule service success at port [{}]", netty4Config.getServerPort());
            //add shutdownHolder
        } catch (Throwable e) {
            logger.error("build server error ", e);
            throw e;
        } finally {
        }
    }

    public void clean() {
        logger.info("shutdown schedule server ");
        shutdown();
    }

    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
