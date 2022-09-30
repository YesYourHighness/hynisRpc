package com.hynis.rpc.server.config;

import com.hynis.rpc.common.codec.RpcMsgDecoder;
import com.hynis.rpc.common.codec.RpcMsgEncoder;
import com.hynis.rpc.common.entity.RpcRequest;
import com.hynis.rpc.common.entity.RpcResponse;
import com.hynis.rpc.common.serialize.protostuff.ProtostuffUtils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hynis
 * @date 2022/2/22 22:52
 * 初始化通道配置
 */
@Slf4j
public class NettyServerConfig extends ChannelInitializer<SocketChannel> {
    /**
     * 初始化通道，给通道加载各种加载器
     * @param socketChannel
     */
    @Override
    protected void initChannel(SocketChannel socketChannel) {
        log.info("Initial channel ...");
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast(new RpcMsgEncoder(RpcRequest.class,new ProtostuffUtils()));
        pipeline.addLast(new RpcMsgDecoder(RpcResponse.class,new ProtostuffUtils()));
    }
}
