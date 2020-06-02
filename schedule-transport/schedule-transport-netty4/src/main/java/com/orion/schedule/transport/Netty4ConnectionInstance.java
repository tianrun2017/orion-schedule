package com.orion.schedule.transport;

import com.orion.schedule.client.ConnectionInstance;
import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.enums.ConnectionState;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/5 16:14
 * @Version 1.0.0
 */
@Data
public class Netty4ConnectionInstance implements ConnectionInstance {

    private Logger logger = LoggerFactory.getLogger(Netty4ConnectionInstance.class);

    private String remoteServer;
    private int port;
    private Channel channel;
    private ConnectionState connectionState = ConnectionState.INIT;

    @Override
    public boolean sendData(ScheduleTaskMsg scheduleTaskMsg) throws Exception {
        logger.debug("channel status [{}] [{}] [{}] [{}] ", channel.isActive(), channel.isOpen(), channel.isRegistered(), channel.isWritable());
        if (!channel.isOpen() || !channel.isActive()) {
            logger.error("server status is not correct ");
            return false;
        }
        ChannelFuture sync = channel.writeAndFlush(scheduleTaskMsg).sync();
        return sync.isSuccess();
    }

    @Override
    public boolean sendCommand(ScheduleTaskMsg scheduleTaskMsg) throws Exception {
        logger.debug("channel status [{}] [{}] [{}] [{}] ", channel.isActive(), channel.isOpen(), channel.isRegistered(), channel.isWritable());
        if (!channel.isOpen() || !channel.isActive()) {
            logger.error("server status is not correct ");
            return false;
        }
        ChannelFuture sync = channel.writeAndFlush(scheduleTaskMsg).sync();
        return sync.isSuccess();
    }

    @Override
    public boolean disConnection() {
        try {
            ChannelFuture sync = channel.disconnect().sync();
            logger.info("disconnect from server [{}] result is [{}]", channel.remoteAddress(), sync.isSuccess());
            return sync.isSuccess();
        } catch (Throwable e) {
            logger.error(String.format("disconnect fail for server [%s] port [%s]", remoteServer, port), e);
        }
        return false;
    }

    @Override
    public String getRemoteServer() {
        return remoteServer + "_" + port;
    }

    @Override
    public ConnectionState getConnectionState() {
        return this.connectionState;
    }

    @Override
    public void stateTrans(ConnectionState newState) {
        if (connectionState != ConnectionState.REMOVED) {
            this.connectionState = newState;
        }
    }
}
