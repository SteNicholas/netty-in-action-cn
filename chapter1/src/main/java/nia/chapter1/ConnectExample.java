package nia.chapter1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * ChannelFuture用于在执行异步操作的时候使用。监听器的回调方法operationComplete()将会在对应的操作完成时被调用，然后监听器可以判断该操作是成功地完成了还是出错了。如果是后者，检索产生的Throwable。由ChannelFutureListener提供的通知机制消除手动检查对应的操作是否完成的必要。
 * 每个Netty的出站I/O操作都将返回一个ChannelFuture；也就是说，它们都不会阻塞。
 */

/**
 * Created by kerr.
 * <p>
 * 代码清单 1-3 异步地建立连接
 * <p>
 * 代码清单 1-4 回调实战
 */
public class ConnectExample {
    private static final Channel CHANNEL_FROM_SOMEWHERE = new NioSocketChannel();

    /**
     * 代码清单 1-3 异步地建立连接
     * <p>
     * 代码清单 1-4 回调实战
     */
    public static void connect() {
        Channel channel = CHANNEL_FROM_SOMEWHERE; //reference form somewhere
        // Does not block
        //异步地连接到远程节点
        ChannelFuture future = channel.connect(
                new InetSocketAddress("192.168.0.1", 25));
        //注册一个 ChannelFutureListener，以便在操作完成时获得通知
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                //当该监听器被通知连接已经建立的时候，检查操作的状态
                if (future.isSuccess()) {
                    //如果操作是成功的，则创建一个 ByteBuf 以持有数据
                    ByteBuf buffer = Unpooled.copiedBuffer(
                            "Hello", Charset.defaultCharset());
                    //将数据异步地发送到远程节点。返回一个 ChannelFuture
                    ChannelFuture wf = future.channel()
                            .writeAndFlush(buffer);
                    // ...
                } else {
                    //如果发生错误，则访问描述原因的 Throwable
                    Throwable cause = future.cause();
                    cause.printStackTrace();
                }
            }
        });
    }
}