package com.hynis.rpc.server.initializer;

import com.hynis.rpc.common.codec.RpcMsgDecoder;
import com.hynis.rpc.common.codec.RpcMsgEncoder;
import com.hynis.rpc.common.entity.RpcRequest;
import com.hynis.rpc.common.entity.RpcResponse;
import com.hynis.rpc.common.serializer.Serializer;
import com.hynis.rpc.common.serializer.protostuff.ProtostuffSerializer;
import com.hynis.rpc.common.config.Beat;
import com.hynis.rpc.server.call.RpcServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author hynis
 * @date 2022/2/22 22:52
 * 初始化通道配置
 */
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {

    private Map<String, Object> serviceMap;
    private ThreadPoolExecutor threadPoolExecutor;

    public NettyServerInitializer(Map<String, Object> serviceMap, ThreadPoolExecutor threadPoolExecutor) {
        this.serviceMap = serviceMap;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    /**
     * 初始化通道
     * @param socketChannel
     */
    @Override
    protected void initChannel(SocketChannel socketChannel) throws InstantiationException, IllegalAccessException {
        Serializer serializer = ProtostuffSerializer.class.newInstance();

        ChannelPipeline pipeline = socketChannel.pipeline();
        /**
         * 入站处理器：入站处理器负责处理从网络到应用程序的数据。
         * 1. IdleStateHandler：负责处理连接的空闲状态，当读、写空闲时会触发对应的事件
         *  1.1 构造需要四个参数：读空闲时间、写空闲时间、读写空闲时间、时间单位
         * 2. LengthFieldBasedFrameDecoder：负责处理基于长度字段的数据帧，数据帧的格式由RpcRequest类实现
         *  2.1 构造需要三个参数：数据帧最大长度、偏移量（长度字段本身占用的字节数）、长度字段本身占用的字节数、帧长度字段的值与帧的实际长度之间的差值、跳过的字节数
         * 3. RpcMsgDecoder：解码器
         * 4. RpcServerHandler：核心，负责执行客户端所需要的服务
         */
        pipeline.addLast(new IdleStateHandler(0, 0, Beat.BEAT_TIMEOUT, TimeUnit.SECONDS));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        pipeline.addLast(new RpcMsgDecoder(RpcResponse.class, serializer));
        pipeline.addLast(new RpcServerHandler(serviceMap, threadPoolExecutor));
        /**
         * 出站处理器：出站处理器负责处理从应用程序到网络的数据。
         * 1. RpcMsgEncoder: 编码器
         */
        pipeline.addLast(new RpcMsgEncoder(RpcRequest.class, serializer));

    }
}
