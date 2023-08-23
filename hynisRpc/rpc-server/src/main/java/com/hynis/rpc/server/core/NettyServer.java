package com.hynis.rpc.server.core;

import com.hynis.rpc.common.util.ServiceUtil;
import com.hynis.rpc.common.util.ThreadPoolUtil;
import com.hynis.rpc.server.initializer.NettyServerInitializer;
import com.hynis.rpc.server.config.ServerConfig;
import com.hynis.rpc.server.register.ServiceRegister;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author hynis
 * @date 2022/2/22 21:11
 * NettyServer启动与关闭
 */
@Slf4j
public class NettyServer extends Server {
    /**
     * 服务器的监听地址
     */
    private String serverAddress;
    /**
     * 服务注册地址（即Zookeeper的地址）
     */
    private ServiceRegister serviceRegister;
    /**
     * 主线程
     */
    private Thread thread;
    /**
     * 存放该服务地址提供的所有服务，key为接口名+版本号，value为实现类存于map
     */
    private Map<String, Object> serviceMap = new HashMap<>();

    public NettyServer(String serverAddress, String registerAddress) {
        this.serverAddress = serverAddress;
        this.serviceRegister = new ServiceRegister(registerAddress);
    }

    /**
     * 注册服务：将服务（接口名，版本号，实现类），按key为接口名+版本号，value为实现类存于map
     * @param interfaceName
     * @param version
     * @param serviceBean
     */
    public void addService(String interfaceName, String version, Object serviceBean) {
        log.info("Adding service, interface: {}, version: {}, bean：{}", interfaceName, version, serviceBean);
        String serviceKey = ServiceUtil.makeServiceKey(interfaceName, version);
        serviceMap.put(serviceKey, serviceBean);
    }

    @Override
    public void start() {
        thread = new Thread(new Runnable() {
            ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtil
                    .createThreadPool(NettyServer.class.getSimpleName(), 16, 32, 60);

            @Override
            public void run() {
                // 从服务地址获得host与port
                String[] array = serverAddress.split(":");
                String host = array[0];
                int port = Integer.parseInt(array[1]);

                // boss：负责ACCEPT
                NioEventLoopGroup bossGroup = new NioEventLoopGroup();
                // worker：负责READ、WRITE请求
                NioEventLoopGroup workerGroup = new NioEventLoopGroup();
                // 入站出站处理器
                NettyServerInitializer nettyServerInitializer = new NettyServerInitializer(serviceMap, threadPoolExecutor);
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
                            // 开启 Nagle算法：该算法要求对于小的packet要一起发送，减小延迟
                            .childOption(ChannelOption.TCP_NODELAY, true)
                            // 初始化通道
                            .childHandler(nettyServerInitializer)
                            .bind(host, port)
                            .sync();
                    if (serviceRegister != null) {
                        serviceRegister.registerService(host, port, serviceMap);
                    }
                    log.info("Netty server started successfully in the port={}", ServerConfig.SERVER_PORT);
                    future.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    log.error("Netty server start error={}", e.getMessage());
                } finally {
                    serviceRegister.unregisterService();
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            }
        });
        thread.start();
    }

    @Override
    public void stop() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

}
