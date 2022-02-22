package com.hynis.rpc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Value;

import java.nio.channels.SocketChannel;

/**
 * @author hynis
 * @date 2022/2/22 21:11
 */
public class NettyServer extends Server {

    private final int SERVER_PORT = 8080;

    @Override
    public void start() {
        // boss：负责ACCEPT
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        // worker：负责READ、WRITE请求
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        // service_handler：负责处理RPC请求
        RpcNettyServerInitializer rpcNettyServerInitializer = new RpcNettyServerInitializer();
        // 启动
        try {
            ChannelFuture future = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    // 使用Nio通道
                    .channel(NioServerSocketChannel.class)
                    // bossGroup 设置
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // TCP 心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 开启 Nagle算法：该算法要求对于小的packet要一起发送
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 初始化通道
                    .childHandler(rpcNettyServerInitializer)
                    .bind(SERVER_PORT)
                    .sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void stop() {

    }

}
