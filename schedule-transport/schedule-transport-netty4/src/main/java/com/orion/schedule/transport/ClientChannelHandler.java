package com.orion.schedule.transport;

import com.orion.schedule.codec.ScheduleCodec;
import com.orion.schedule.domain.ScheduleTaskMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/5 16:50
 * @Version 1.0.0
 */
public class ClientChannelHandler extends ChannelInitializer<SocketChannel> {
    private ScheduleCodec scheduleCodec = null;
    private int heartInterval;

    public ClientChannelHandler(ScheduleCodec scheduleCodec, int heartInterval) {
        this.scheduleCodec = scheduleCodec;
        this.heartInterval = heartInterval;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
        pipeline.addLast(new LengthFieldPrepender(4));
        pipeline.addLast(buildDecoder());
        pipeline.addLast(buildEncoder());
        //检测时间比服务端小1秒
        pipeline.addLast(new IdleStateHandler(0, heartInterval > 1 ? heartInterval - 1 : 0, 0, TimeUnit.SECONDS));
        pipeline.addLast(new ClientHeartBeatHandler());
    }

    private ChannelHandler buildEncoder() {
        MessageToByteEncoder messageToByteEncoder = new MessageToByteEncoder<ScheduleTaskMsg>() {
            @Override
            protected void encode(
                    ChannelHandlerContext channelHandlerContext, ScheduleTaskMsg scheduleTaskMsg, ByteBuf byteBuf)
                    throws Exception {
                byte[] write = scheduleCodec.encode(scheduleTaskMsg);
                byteBuf.writeBytes(write);
            }
        };
        return messageToByteEncoder;
    }

    private ChannelHandler buildDecoder() {
        ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
            @Override
            protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list)
                    throws Exception {
                final int len = byteBuf.readableBytes();
                byte[] bytes = new byte[len];
                byteBuf.getBytes(byteBuf.readerIndex(), bytes, 0, len);
                Object decode = scheduleCodec.decode(bytes);
                byteBuf.skipBytes(len);
                list.add(decode);
            }
        };
        return decoder;
    }
}

