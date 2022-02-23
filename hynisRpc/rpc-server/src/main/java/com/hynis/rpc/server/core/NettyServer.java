package com.hynis.rpc.server.core;

import com.hynis.rpc.server.config.NettyServerConfig;
import com.hynis.rpc.server.config.ServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hynis
 * @date 2022/2/22 21:11
 */
@Slf4j
public class NettyServer extends Server {


    @Override
    public void start() {
        // boss：负责ACCEPT
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        // worker：负责READ、WRITE请求
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        // service_handler：负责处理RPC请求
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        // 启动
        try {
            ChannelFuture future = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    // 使用Nio通道
                    .channel(NioServerSocketChannel.class)
                    // bossGroup 设置握手请求队列大小
                    .option(ChannelOption.SO_BACKLOG, ServerConfig.SO_BACKLOG_SIZE)
                    // TCP 心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 开启 Nagle算法：该算法要求对于小的packet要一起发送
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 初始化通道
                    .childHandler(nettyServerConfig)
                    .bind(ServerConfig.SERVER_PORT)
                    .sync();
            log.info("Netty server started successfully in the port={}", ServerConfig.SERVER_PORT);
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Netty server start error={}", e.getMessage());
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void stop() {

    }

}
