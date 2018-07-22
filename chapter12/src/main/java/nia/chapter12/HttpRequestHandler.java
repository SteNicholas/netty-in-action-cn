package nia.chapter12;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * WebSocket协议旨在为Web上的双向数据传输问题提供一个切实可行的解决方案，使得客户端和服务器之间可以在任意时刻传输消息，要求异步处理消息回执。
 */

/**
 * 代码清单 12-1 HTTPRequestHandler
 * 提供用于访问聊天室并显示由连接的客户端发送的消息的网页。
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
//扩展 SimpleChannelInboundHandler 以处理 FullHttpRequest 消息
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String wsUri;
    private static final File INDEX;

    static {
        URL location = HttpRequestHandler.class
                .getProtectionDomain()
                .getCodeSource().getLocation();
        try {
            String path = location.toURI() + "index.html";
            path = !path.contains("file:") ? path : path.substring(5);
            INDEX = new File(path);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(
                    "Unable to locate index.html", e);
        }
    }

    public HttpRequestHandler(String wsUri) {
        this.wsUri = wsUri;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx,
                             FullHttpRequest request) throws Exception {
        //(1) 如果请求了 WebSocket 协议升级，则增加引用计数（调用 retain()方法），并将它传递给下一 个 ChannelInboundHandler
        if (wsUri.equalsIgnoreCase(request.getUri())) {
            //如果该HTTP请求指向地址为/ws的URI，那么HttpRequestHandler将调用FullHttpRequest对象上的retain()方法，并通过调用fireChannelRead(msg)方法将它转发给下一个ChannelInboundHandler。之所以需要调用retain()方法是因为调用channelRead()方法完成之后，它将调用FullHttpRequest对象上的release()方法以释放它的资源。
            ctx.fireChannelRead(request.retain());
        } else {
            //(2) 处理 100 Continue 请求以符合 HTTP 1.1 规范
            if (HttpHeaders.is100ContinueExpected(request)) {
                //如果客户端发送HTTP 1.1头信息Expect:100-continue，那么HttpRequestHandler将会发送一个100 Continue响应。
                send100Continue(ctx);
            }
            //读取 index.html
            RandomAccessFile file = new RandomAccessFile(INDEX, "r");
            HttpResponse response = new DefaultHttpResponse(
                    request.getProtocolVersion(), HttpResponseStatus.OK);
            response.headers().set(
                    HttpHeaders.Names.CONTENT_TYPE,
                    "text/html; charset=UTF-8");
            boolean keepAlive = HttpHeaders.isKeepAlive(request);
            //如果请求了keep-alive，则添加所需要的 HTTP 头信息
            if (keepAlive) {
                response.headers().set(
                        HttpHeaders.Names.CONTENT_LENGTH, file.length());
                response.headers().set(HttpHeaders.Names.CONNECTION,
                        HttpHeaders.Values.KEEP_ALIVE);
            }
            //(3) 将 HttpResponse 写到客户端。在该HTTP头信息被设置之后，HttpRequestHandler将会写回一个HttpResponse给客户端。
            ctx.write(response);
            //(4) 将 index.html 写到客户端。如果不需要加密和压缩，那么可以通过将index.html的内存存储到DefaultFileRegion中来达到最佳效率，将会利用零拷贝特性来进行内容的传输。为此检查是否有SslHandler存在于在ChannelPipeline中，否则使用ChunkedNioFile。
            if (ctx.pipeline().get(SslHandler.class) == null) {
                ctx.write(new DefaultFileRegion(
                        file.getChannel(), 0, file.length()));
            } else {
                ctx.write(new ChunkedNioFile(file.getChannel()));
            }
            //(5) 写 LastHttpContent 并冲刷至客户端。HttpRequestHandler将写一个LastHttpContent来标记响应的结束。
            ChannelFuture future = ctx.writeAndFlush(
                    LastHttpContent.EMPTY_LAST_CONTENT);
            //(6) 如果没有请求keep-alive，则在写操作完成后关闭 Channel。如果没有请求keep-alive，那么HttpRequestHandler将会添加一个ChannelFutureListener到最后一次写出动作的ChannelFuture,并关闭该连接。
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}