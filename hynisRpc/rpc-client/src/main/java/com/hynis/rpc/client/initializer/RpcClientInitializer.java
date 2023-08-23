package com.hynis.rpc.client.initializer;

import com.hynis.rpc.client.handler.RpcClientHandler;
import com.hynis.rpc.common.codec.RpcMsgDecoder;
import com.hynis.rpc.common.codec.RpcMsgEncoder;
import com.hynis.rpc.common.config.Beat;
import com.hynis.rpc.common.entity.RpcRequest;
import com.hynis.rpc.common.entity.RpcResponse;
import com.hynis.rpc.common.serializer.Serializer;
import com.hynis.rpc.common.serializer.protostuff.ProtostuffSerializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author hynis
 * @date 2023/8/17 20:13
 *
 * 客户端Netty处理器配置
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        Serializer serializer = ProtostuffSerializer.class.newInstance();
        ChannelPipeline cp = socketChannel.pipeline();

        // 入站处理器
        cp.addLast(new IdleStateHandler(0, 0, Beat.BEAT_INTERVAL, TimeUnit.SECONDS));
        cp.addLast(new RpcMsgEncoder(RpcRequest.class, serializer));
        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        cp.addLast(new RpcClientHandler());
        // 出站处理器
        cp.addLast(new RpcMsgDecoder(RpcResponse.class, serializer));
    }
}
