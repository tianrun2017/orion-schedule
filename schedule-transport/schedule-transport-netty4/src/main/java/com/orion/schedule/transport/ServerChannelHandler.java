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
 * @Date 2019/5/31 10:25
 * @Version 1.0.0
 */
public class ServerChannelHandler extends ChannelInitializer<SocketChannel> {

    private ScheduleCodec scheduleCodec = null;
    private int idleTime;

    public ServerChannelHandler(ScheduleCodec codec, int idleTime) {
        this.scheduleCodec = codec;
        this.idleTime = idleTime;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
        pipeline.addLast(new LengthFieldPrepender(4));
        pipeline.addLast(buildDecoder());
        pipeline.addLast(buildEncoder());
        pipeline.addLast(new IdleStateHandler(idleTime, 0, 0, TimeUnit.SECONDS));
        pipeline.addLast(new ScheduleTaskHandler());
    }

    private ChannelHandler buildEncoder() {
        MessageToByteEncoder messageToByteEncoder = new MessageToByteEncoder<ScheduleTaskMsg>() {
            @Override
            protected void encode(ChannelHandlerContext channelHandlerContext, ScheduleTaskMsg scheduleTaskMsg, ByteBuf byteBuf)
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
