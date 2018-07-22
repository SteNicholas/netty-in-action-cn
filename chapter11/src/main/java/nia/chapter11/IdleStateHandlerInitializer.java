package nia.chapter11;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

/**
 * 空闲的连接和超时
 * IdleStateHandler:当连接的空闲时间太长时，将会触发一个IdleStateEvent事件。然后，你可以通过在你的ChannelInboundHandler中重写userEventTriggered()方法来处理该IdleStateEvent事件。
 * ReadTimeoutHandler:如果在指定的时间间隔内没有收到任何的入站数据，则抛出一个ReadTimeoutException并关闭对应的Channel。可以通过重写你的ChannelHandler中的exceptionCaught()方法来检测该ReadTimeoutException。
 * WriteTimeoutHandler:如果在指定的时间间隔内没有任何出站数据写入，则抛出一个WriteTimeoutException并关闭对应的Channel。可以通过重写你的ChannelHandler中的exceptionCaught()方法来检测该WriteTimeoutException。
 */

/**
 * 代码清单 11-7 发送心跳
 * 如何使用IdleStateHandler来测试远程节点是否仍然还活着，并且在它失活时通过关闭连接来释放资源。
 * 如果连接超过60秒没有接收或者发送任何的数据，那么IdleStateHandler(1)将会使用一个IdleStateEvent事件来调用fireUserEventTriggered()方法。HeartbeatHandler实现userEventTriggered()方法，如果这个方法检测到IdleStateEvent事件，它将会发送心跳消息，并且添加一个将在发送操作失败时关闭该连接的ChannelFutureListener(2)。
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class IdleStateHandlerInitializer extends ChannelInitializer<Channel> {
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(
                //(1) IdleStateHandler 将在被触发时发送一个IdleStateEvent 事件
                new IdleStateHandler(0, 0, 60, TimeUnit.SECONDS));
        //将一个 HeartbeatHandler 添加到ChannelPipeline中
        pipeline.addLast(new HeartbeatHandler());
    }

    //实现 userEventTriggered() 方法以发送心跳消息
    public static final class HeartbeatHandler
            extends ChannelInboundHandlerAdapter {
        //发送到远程节点的心跳消息
        private static final ByteBuf HEARTBEAT_SEQUENCE =
                Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(
                        "HEARTBEAT", CharsetUtil.ISO_8859_1));

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx,
                                       Object evt) throws Exception {
            //(2) 发送心跳消息，并在发送失败时关闭该连接
            if (evt instanceof IdleStateEvent) {
                ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate())
                        .addListener(
                                ChannelFutureListener.CLOSE_ON_FAILURE);
            } else {
                //不是 IdleStateEvent 事件，所以将它传递给下一个 ChannelInboundHandler
                super.userEventTriggered(ctx, evt);
            }
        }
    }
}