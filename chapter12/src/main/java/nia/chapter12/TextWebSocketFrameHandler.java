package nia.chapter12;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * 处理WebSocket帧
 * 帧类型:
 * BinaryWebSocketFrame:包含二进制数据。
 * TextWebSocketFrame:包含文本数据。
 * ContinuationWebSocketFrame:包含属于上一个BinaryWebSocketFrame或TextWebSocketFrame的文本数据或者二进制数据。
 * CloseWebSocketFrame:表示一个CLOSE请求，包含一个关闭的状态码和关闭的原因。
 * PingWebSocketFrame:请求传输一个PongWebSocketFrame。
 * PongWebSocketFrame:作为一个对于PingWebSocketFrame的响应被发送。
 */

/**
 * 代码清单 12-2 处理文本帧
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
//扩展 SimpleChannelInboundHandler，并处理 TextWebSocketFrame 消息
public class TextWebSocketFrameHandler
        extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final ChannelGroup group;

    public TextWebSocketFrameHandler(ChannelGroup group) {
        this.group = group;
    }

    //重写 userEventTriggered()方法以处理自定义事件
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx,
                                   Object evt) throws Exception {
        //如果该事件表示握手成功，则从该 ChannelPipeline 中移除HttpRequest-Handler，因为将不会接收到任何HTTP消息了
        if (evt == WebSocketServerProtocolHandler
                .ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            ctx.pipeline().remove(HttpRequestHandler.class);
            //(1) 通知所有已经连接的 WebSocket 客户端新的客户端已经连接上了。当和新客户端的WebSocket握手成功完成之后，它将通过把通知消息写到ChannelGroup中的所有Channel来通知所有已经连接的客户端。
            group.writeAndFlush(new TextWebSocketFrame(
                    "Client " + ctx.channel() + " joined"));
            //(2) 将新的 WebSocket Channel 添加到 ChannelGroup 中，以便它可以接收到所有的消息
            group.add(ctx.channel());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx,
                             TextWebSocketFrame msg) throws Exception {
        //(3) 增加消息的引用计数，并将它写到 ChannelGroup 中所有已经连接的客户端。如果接收到TextWebSocketFrame消息，TextWebSocketFrameHandler将调用TextWebSocketFrame消息上的retain()方法，并使用writeAndFlush()方法来将它传输给ChannelGroup，以便所有已经连接的WebSocket Channel都将接收到它。
        //对于retain()方法的调用是必需的，因为当channelRead0()方法返回时TextWebSocketFrame的引用计数将会被减少。由于所有的操作都是异步的，因此writeAndFlush()方法可能会在channelRead0()方法返回之后完成，而且它绝对不能访问一个已经失效的引用。
        group.writeAndFlush(msg.retain());
    }
}