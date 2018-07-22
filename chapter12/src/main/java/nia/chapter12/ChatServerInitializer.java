package nia.chapter12;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 代码清单 12-3 初始化 ChannelPipeline
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
//扩展了 ChannelInitializer
public class ChatServerInitializer extends ChannelInitializer<Channel> {
    private final ChannelGroup group;

    public ChatServerInitializer(ChannelGroup group) {
        this.group = group;
    }

    @Override
    //将所有需要的 ChannelHandler 添加到 ChannelPipeline 中
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        //HttpServerCodec将字节解码为HttpRequest、HttpContent和LastHttpContent，并将HttpRequest、HttpContent和LastHttpContent编码为字节。
        pipeline.addLast(new HttpServerCodec());
        //ChunkedWriteHandler写入一个文件的内容。
        pipeline.addLast(new ChunkedWriteHandler());
        //HttpObjectAggregator将一个HttpMessage和跟随它的多个HttpContent聚合为单个FullHttpRequest或者FullHttpResponse(取决于它是被用来处理请求还是响应)。安装这个之后ChannelPipeline中的下一个ChannelHandler将只会接受到完整的HTTP请求或响应。
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
        //HttpRequestHandler处理FullHttpRequest(那些不发送到/ws URI的请求)
        pipeline.addLast(new HttpRequestHandler("/ws"));
        //WebSocketServerProtocolHandler按照WebSocket规范的要求，处理WebSocket升级握手、PingWebSocketFrame、PongWebSocketFrame和CloseWebSocketFrame。WebSocketServerProtocolHandler处理所有委托管理的WebSocket帧类型以及升级握手本身。如果握手成功，那么所需的ChannelHandler将会被添加到ChannelPipeline中，而那些不再需要的ChannelHandler则将会被移除。
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
        //TextWebSocketFrameHandler处理TextWebSocketFrame和握手完成事件。
        pipeline.addLast(new TextWebSocketFrameHandler(group));
    }
}