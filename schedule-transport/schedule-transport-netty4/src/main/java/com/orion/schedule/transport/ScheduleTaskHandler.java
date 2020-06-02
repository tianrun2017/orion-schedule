package com.orion.schedule.transport;

import com.orion.schedule.common.util.InetUtils;
import com.orion.schedule.domain.ScheduleTaskMsg;
import com.orion.schedule.enums.Command;
import com.orion.schedule.service.MessageProcessService;
import com.orion.schedule.util.ScheduleContextUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @Description 处理请求入参事件
 * @Author beedoorwei
 * @Date 2019/5/30 10:47
 * @Version 1.0.0
 */
public class ScheduleTaskHandler extends ChannelDuplexHandler {

    private Logger logger = LoggerFactory.getLogger(ScheduleTaskHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ScheduleTaskMsg scheduleTaskMsg = (ScheduleTaskMsg) msg;
        if (scheduleTaskMsg.getCommand() == Command.PING) {
            //ping消息不做任务处理
            logger.debug("receive ping msg from channel [{}] server [{}]", ctx.channel().id(), ctx.channel().remoteAddress());
            return;
        }
        logger.info("receive cid [{}] cmd [{}] node [{}] stid [{}] stepId [{}]", scheduleTaskMsg.getCommandId(), scheduleTaskMsg.getCommand(), scheduleTaskMsg.getCurrentNodeName(), scheduleTaskMsg.getScheduleTraceId(), scheduleTaskMsg.getStepTraceId());
        MessageProcessService messageProcessService = ScheduleContextUtils.getBean(MessageProcessService.class);
        messageProcessService.process(scheduleTaskMsg);
    }

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
            //检测空闲时间
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                logger.warn("there is no msg from channel [{}] server [{}],close it", ctx.channel().id(), ctx.channel().remoteAddress());
                //没有收到客户端的消息
                ctx.channel().close();
            }
        }
        ctx.channel().id();
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(String.format("client connection [%s] ,channelId [%s] instance exception,close it ", ctx.channel().remoteAddress(), ctx.channel().id()), cause);
        ctx.channel().close();
//		super.exceptionCaught(ctx, cause);
    }
}
