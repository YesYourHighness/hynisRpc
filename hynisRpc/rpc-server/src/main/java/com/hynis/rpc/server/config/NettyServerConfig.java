package com.hynis.rpc.server.config;

import com.hynis.rpc.common.codec.RpcMsgDecoder;
import com.hynis.rpc.common.codec.RpcMsgEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * @author hynis
 * @date 2022/2/22 22:52
 * 初始化通道配置
 */
public class NettyServerConfig extends ChannelInitializer<SocketChannel> {

    /**
     * 初始化通道
     * @param socketChannel
     */
    @Override
    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast(new RpcMsgEncoder());
        pipeline.addLast(new RpcMsgDecoder());
        pipeline.addLast();
        pipeline.addLast();
    }
}
