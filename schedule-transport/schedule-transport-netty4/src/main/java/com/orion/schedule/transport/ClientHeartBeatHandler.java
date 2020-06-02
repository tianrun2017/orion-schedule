package com.orion.schedule.transport;

import com.orion.schedule.common.util.InetUtils;
import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.enums.Command;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/6 6:10
 * @Version 1.0.0
 */
public class ClientHeartBeatHandler extends ChannelDuplexHandler {

    private Logger logger = LoggerFactory.getLogger(ClientHeartBeatHandler.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            String remoteAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
            String localAddress = ((InetSocketAddress) ctx.channel().localAddress()).getAddress().getHostAddress();
            if (StringUtils.equals(remoteAddress, localAddress)
                    || StringUtils.equals(remoteAddress, InetUtils.getSelfIp())) {
                //TODO 自己不给自己发送心跳消息,一台机器部署多个实例的先不考虑
                return;
            }
            ScheduleTaskMsg<Object> pingMsg = new ScheduleTaskMsg<>();
            pingMsg.setStartHost(InetUtils.getSelfIp());
            pingMsg.setCommand(Command.PING);
            ctx.channel().writeAndFlush(pingMsg);
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("heart beat exception ", cause);
//        super.exceptionCaught(ctx, cause);
    }
}
