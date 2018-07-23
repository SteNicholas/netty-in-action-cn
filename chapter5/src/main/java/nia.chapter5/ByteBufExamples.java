package nia.chapter5;

import io.netty.buffer.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ByteProcessor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Random;

import static io.netty.channel.DummyChannelHandlerContext.DUMMY_INSTANCE;

/**
 * Created by kerr.
 * <p>
 * 代码清单 5-1 支撑数组
 * <p>
 * 代码清单 5-2 访问直接缓冲区的数据
 * <p>
 * 代码清单 5-3 使用 ByteBuffer 的复合缓冲区模式
 * <p>
 * 代码清单 5-4 使用 CompositeByteBuf 的复合缓冲区模式
 * <p>
 * 代码清单 5-5 访问 CompositeByteBuf 中的数据
 * <p>
 * 代码清单 5-6 访问数据
 * <p>
 * 代码清单 5-7 读取所有数据
 * <p>
 * 代码清单 5-8 写数据
 * <p>
 * 代码清单 5-9 使用 ByteBufProcessor 来寻找\r
 * <p>
 * 代码清单 5-10 对 ByteBuf 进行切片
 * <p>
 * 代码清单 5-11 复制一个 ByteBuf
 * <p>
 * 代码清单 5-12 get()和 set()方法的用法
 * <p>
 * 代码清单 5-13 ByteBuf 上的 read()和 write()操作
 * <p>
 * 代码清单 5-14 获取一个到 ByteBufAllocator 的引用
 * <p>
 * 代码清单 5-15 引用计数
 * <p>
 * 代码清单 5-16 释放引用计数的对象
 */
public class ByteBufExamples {
    private final static Random random = new Random();
    private static final ByteBuf BYTE_BUF_FROM_SOMEWHERE = Unpooled.buffer(1024);
    private static final Channel CHANNEL_FROM_SOMEWHERE = new NioSocketChannel();
    private static final ChannelHandlerContext CHANNEL_HANDLER_CONTEXT_FROM_SOMEWHERE = DUMMY_INSTANCE;

    /**
     * 代码清单 5-1 支撑数组
     * 堆缓冲区将数据存储在JVM的堆空间中，能在没有使用池化的情况下提供快速的分配和释放，适合于有遗留的数据需要处理的情况。
     */
    public static void heapBuffer() {
        ByteBuf heapBuf = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        //检查 ByteBuf 是否有一个支撑数组
        if (heapBuf.hasArray()) {
            //如果有，则获取对该数组的引用
            byte[] array = heapBuf.array();
            //计算第一个字节的偏移量
            int offset = heapBuf.arrayOffset() + heapBuf.readerIndex();
            //获得可读字节数
            int length = heapBuf.readableBytes();
            //使用数组、偏移量和长度作为参数调用你的方法
            handleArray(array, offset, length);
        }
    }

    /**
     * 代码清单 5-2 访问直接缓冲区的数据
     * 直接缓冲区用于对象创建的内存分配永远都来自于堆中，为了避免在每次调用本地I/O操作之前(或者之后)将缓冲区的内容复制到一个中间缓冲区（或者从中间缓冲区把内容复制到缓冲区）。直接缓冲区的内容将驻留在常规的会被垃圾回收的堆之外。
     */
    public static void directBuffer() {
        ByteBuf directBuf = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        //检查 ByteBuf 是否由数组支撑。如果不是，则这是一个直接缓冲区
        if (!directBuf.hasArray()) {
            //获取可读字节数
            int length = directBuf.readableBytes();
            //分配一个新的数组来保存具有该长度的字节数据
            byte[] array = new byte[length];
            //将字节复制到该数组
            directBuf.getBytes(directBuf.readerIndex(), array);
            //使用数组、偏移量和长度作为参数调用你的方法
            handleArray(array, 0, length);
        }
    }

    /**
     * 代码清单 5-3 使用 ByteBuffer 的复合缓冲区模式
     * 复合缓冲区为多个ByteBuf提供一个聚合视图，根据需要添加或者删除ByteBuf实例。CompositeByteBuf提供一个将多个缓冲区表示为单个合并缓冲区的虚拟表示。
     */
    public static void byteBufferComposite(ByteBuffer header, ByteBuffer body) {
        // Use an array to hold the message parts
        ByteBuffer[] message = new ByteBuffer[]{header, body};

        // Create a new ByteBuffer and use copy to merge the header and body
        ByteBuffer message2 =
                ByteBuffer.allocate(header.remaining() + body.remaining());
        message2.put(header);
        message2.put(body);
        message2.flip();
    }


    /**
     * 代码清单 5-4 使用 CompositeByteBuf 的复合缓冲区模式
     */
    public static void byteBufComposite() {
        CompositeByteBuf messageBuf = Unpooled.compositeBuffer();
        ByteBuf headerBuf = BYTE_BUF_FROM_SOMEWHERE; // can be backing or direct
        ByteBuf bodyBuf = BYTE_BUF_FROM_SOMEWHERE;   // can be backing or direct
        //将 ByteBuf 实例追加到 CompositeByteBuf
        messageBuf.addComponents(headerBuf, bodyBuf);
        //...
        //删除位于索引位置为 0（第一个组件）的 ByteBuf
        messageBuf.removeComponent(0); // remove the header
        //循环遍历所有的 ByteBuf 实例
        for (ByteBuf buf : messageBuf) {
            System.out.println(buf.toString());
        }
    }

    /**
     * 代码清单 5-5 访问 CompositeByteBuf 中的数据
     * Netty使用CompositeByteBuf优化套接字I/O操作，尽可能地消除由JDK的缓冲区实现所导致的性能以及内存使用率的惩罚。
     */
    public static void byteBufCompositeArray() {
        CompositeByteBuf compBuf = Unpooled.compositeBuffer();
        //获得可读字节数
        int length = compBuf.readableBytes();
        //分配一个具有可读字节数长度的新数组
        byte[] array = new byte[length];
        //将字节读到该数组中
        compBuf.getBytes(compBuf.readerIndex(), array);
        //使用偏移量和长度作为参数使用该数组
        handleArray(array, 0, array.length);
    }

    /**
     * 代码清单 5-6 访问数据
     * ByteBuf的索引是从零开始的：第一个字节的索引是0，最后一个字节的索引总是capacity()-1。使用需要一个索引值参数的方法（的其中）之一来访问数据既不会改变readerIndex也不会改变writerIndex。通过调用readerIndex(index)或者writerIndex(index)来手动移动这两者。
     */
    public static void byteBufRelativeAccess() {
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        for (int i = 0; i < buffer.capacity(); i++) {
            byte b = buffer.getByte(i);
            System.out.println((char) b);
        }
    }

    /**
     * 代码清单 5-7 读取所有数据
     */
    public static void readAllData() {
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        while (buffer.isReadable()) {
            System.out.println(buffer.readByte());
        }
    }

    /**
     * 代码清单 5-8 写数据
     */
    public static void write() {
        // Fills the writable bytes of a buffer with random integers.
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        while (buffer.writableBytes() >= 4) {
            buffer.writeInt(random.nextInt());
        }
    }

    /**
     * 代码清单 5-9 使用 ByteBufProcessor 来寻找\r
     * <p>
     * use {@link io.netty.buffer.ByteBufProcessor in Netty 4.0.x}
     */
    public static void byteProcessor() {
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        int index = buffer.forEachByte(ByteProcessor.FIND_CR);
    }

    /**
     * 代码清单 5-9 使用 ByteBufProcessor 来寻找\r
     * <p>
     * use {@link io.netty.util.ByteProcessor in Netty 4.1.x}
     */
    public static void byteBufProcessor() {
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        int index = buffer.forEachByte(ByteBufProcessor.FIND_CR);
    }

    /**
     * 代码清单 5-10 对 ByteBuf 进行切片
     */
    public static void byteBufSlice() {
        Charset utf8 = Charset.forName("UTF-8");
        //创建一个用于保存给定字符串的字节的 ByteBuf
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        //创建该 ByteBuf 从索引 0 开始到索引 15 结束的一个新切片
        ByteBuf sliced = buf.slice(0, 15);
        //将打印“Netty in Action”
        System.out.println(sliced.toString(utf8));
        //更新索引 0 处的字节
        buf.setByte(0, (byte) 'J');
        //将会成功，因为数据是共享的，对其中一个所做的更改对另外一个也是可见的
        assert buf.getByte(0) == sliced.getByte(0);
    }

    /**
     * 代码清单 5-11 复制一个 ByteBuf
     */
    public static void byteBufCopy() {
        Charset utf8 = Charset.forName("UTF-8");
        //创建 ByteBuf 以保存所提供的字符串的字节
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        //创建该 ByteBuf 从索引 0 开始到索引 15 结束的分段的副本
        ByteBuf copy = buf.copy(0, 15);
        //将打印“Netty in Action”
        System.out.println(copy.toString(utf8));
        //更新索引 0 处的字节
        buf.setByte(0, (byte) 'J');
        //将会成功，因为数据不是共享的
        assert buf.getByte(0) != copy.getByte(0);
    }

    /**
     * 代码清单 5-12 get()和 set()方法的用法
     * get()和set()操作，从给定的索引开始，并且保持索引不变。
     */
    public static void byteBufSetGet() {
        Charset utf8 = Charset.forName("UTF-8");
        //创建一个新的 ByteBuf以保存给定字符串的字节
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        //打印第一个字符'N'
        System.out.println((char) buf.getByte(0));
        //存储当前的 readerIndex 和 writerIndex
        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();
        //将索引 0 处的字 节更新为字符'B'
        buf.setByte(0, (byte) 'B');
        //打印第一个字符，现在是'B'
        System.out.println((char) buf.getByte(0));
        //将会成功，因为这些操作并不会修改相应的索引
        assert readerIndex == buf.readerIndex();
        assert writerIndex == buf.writerIndex();
    }

    /**
     * 代码清单 5-13 ByteBuf 上的 read()和 write()操作
     * read()和 write()操作，从给定的索引开始，并且会根据已经访问过的字节数对索引进行调整。
     */
    public static void byteBufWriteRead() {
        Charset utf8 = Charset.forName("UTF-8");
        //创建一个新的 ByteBuf 以保存给定字符串的字节
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        //打印第一个字符'N'
        System.out.println((char) buf.readByte());
        //存储当前的readerIndex
        int readerIndex = buf.readerIndex();
        //存储当前的writerIndex
        int writerIndex = buf.writerIndex();
        //将字符 '?'追加到缓冲区
        buf.writeByte((byte) '?');
        assert readerIndex == buf.readerIndex();
        //将会成功，因为 writeByte()方法移动了 writerIndex
        assert writerIndex != buf.writerIndex();
    }

    private static void handleArray(byte[] array, int offset, int len) {
    }

    /**
     * 代码清单 5-14 获取一个到 ByteBufAllocator 的引用
     * 为了降低分配和释放内存的开销，Netty通过interface ByteBufAllocator实现（ByteBuf的）池化，用来分配任意类型的ByteBuf实例。通过Channel（每个都可以有一个不同的ByteBufAllocator实例）或者绑定ChannelHandler的ChannelHandlerContext获取一个到ByteBufAllocator的引用。
     */
    public static void obtainingByteBufAllocatorReference() {
        Channel channel = CHANNEL_FROM_SOMEWHERE; //get reference form somewhere
        //从 Channel 获取一个到ByteBufAllocator 的引用
        ByteBufAllocator allocator = channel.alloc();
        //...
        ChannelHandlerContext ctx = CHANNEL_HANDLER_CONTEXT_FROM_SOMEWHERE; //get reference form somewhere
        //从 ChannelHandlerContext 获取一个到 ByteBufAllocator 的引用
        ByteBufAllocator allocator2 = ctx.alloc();
        //...
    }

    /**
     * 代码清单 5-15 引用计数
     * 引用计数是一种通过在某个对象所持有的资源不再被其他对象引用时释放该对象所持有的资源来优化内存使用和性能的技术。Netty为ByteBuf和ByteBufHolder引入引用计数技术，实现interface ReferenceCounted。一个ReferenceCounted实现的实例将通常以活动的引用计数为1作为开始。只要引用数量大于0，就能保证对象不会被释放。当活动引用的数量减少到0时，该实例就会被释放。引用计数对池化实现降低内存分配的开销。由最后访问（引用计数）对象的那一方来负责将它释放。
     */
    public static void referenceCounting() {
        Channel channel = CHANNEL_FROM_SOMEWHERE; //get reference form somewhere
        //从 Channel 获取ByteBufAllocator
        ByteBufAllocator allocator = channel.alloc();
        //...
        //从 ByteBufAllocator分配一个 ByteBuf
        ByteBuf buffer = allocator.directBuffer();
        //检查引用计数是否为预期的 1
        assert buffer.refCnt() == 1;
        //...
    }

    /**
     * 代码清单 5-16 释放引用计数的对象
     */
    public static void releaseReferenceCountedObject() {
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        //减少到该对象的活动引用。当减少到 0 时，该对象被释放，并且该方法返回 true
        boolean released = buffer.release();
        //...
    }
}