package com.hynis.rpc.client.connect;

import com.hynis.rpc.client.handler.RpcClientHandler;
import com.hynis.rpc.client.initializer.RpcClientInitializer;
import com.hynis.rpc.client.route.RpcLoadBalance;
import com.hynis.rpc.client.route.impl.RpcLoadBalanceRoundRobin;
import com.hynis.rpc.common.protocol.RpcProtocol;
import com.hynis.rpc.common.protocol.RpcServiceInfo;
import com.hynis.rpc.common.util.ThreadPoolUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author hynis
 * @date 2023/8/17 19:08
 */
@Slf4j
@Component
public class ConnectionManager {

    private static class SingletonHolder {
        private static final ConnectionManager instance = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 一个集合类，使用数组实现，采用了COW的思想，适合用在读远远大于写的场景
     * 服务端的服务一般不会发生巨大变化，因此适合使用
     */
    private CopyOnWriteArraySet<RpcProtocol> rpcProtocolSet = new CopyOnWriteArraySet<>();

    /**
     * 对于每一个需要更新的服务信息，都需要连接到对应Server服务器
     * 因此使用一个线程池
     */
    private static ThreadPoolExecutor threadPoolExecutor =
            ThreadPoolUtil.createThreadPool("Client",4,8, 600);

    /**
     * Netty客户端连接服务端
     */
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    /**
     * 已连接的服务器节点Map
     * key: RpcProtocol
     * value: RpcClientHandler 出站处理器
     */
    private Map<RpcProtocol, RpcClientHandler> connectedServerNodeMap = new ConcurrentHashMap<>();


    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();

    private volatile boolean isRunning = true;

    private RpcLoadBalance loadBalance = new RpcLoadBalanceRoundRobin();

    private long waitTimeout = 5000;
    /**
     * 一旦Zookeeper更新了服务信息，此方法就会执行
     * 本方法使用2个集合来管理服务信息和TCP连接，因为建立连接是异步的
     * 1、rpcProtocolSet 存储所有的RpcProtocol
     * 2、connectedServerNodeMap：存储所有已连接到的Server的Map
     * @param serviceList
     */
    public void updateConnectedServer(List<RpcProtocol> serviceList) {
        if (serviceList != null && serviceList.size() > 0) {
            // 更新本地缓存
            HashSet<RpcProtocol> serviceSet = new HashSet<>(serviceList.size());
            for (int i = 0; i < serviceList.size(); ++i) {
                RpcProtocol rpcProtocol = serviceList.get(i);
                serviceSet.add(rpcProtocol);
            }

            // 添加新的服务信息
            for (final RpcProtocol rpcProtocol : serviceSet) {
                if (!rpcProtocolSet.contains(rpcProtocol)) {
                    connectServerNode(rpcProtocol);
                }
            }

            // 关闭和移除服务节点
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                if (!serviceSet.contains(rpcProtocol)) {
                    log.info("Remove invalid service: " + rpcProtocol.toJson());
                    removeAndCloseHandler(rpcProtocol);
                }
            }
        } else {
            // No available service
            log.error("No available service!");
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                removeAndCloseHandler(rpcProtocol);
            }
        }
    }

    /**
     * 更新指定状态的协议
     * @param rpcProtocol
     * @param type
     */
    public void updateConnectedServer(RpcProtocol rpcProtocol, PathChildrenCacheEvent.Type type) {
        if (rpcProtocol == null) {
            return;
        }
        if (type == PathChildrenCacheEvent.Type.CHILD_ADDED && !rpcProtocolSet.contains(rpcProtocol)) {
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
            //TODO We may don't need to reconnect remote server if the server'IP and server'port are not changed
            removeAndCloseHandler(rpcProtocol);
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
            removeAndCloseHandler(rpcProtocol);
        } else {
            throw new IllegalArgumentException("Unknow type:" + type);
        }
    }

    /**
     * 连接对应RpcProtocol的服务端，并将连接存于 connectedServerNodeMap，
     * 在rpcProtocolSet内存放所有的rpcProtocol
     * @param rpcProtocol
     */
    private void connectServerNode(RpcProtocol rpcProtocol) {
        if (rpcProtocol.getServiceInfoList() == null || rpcProtocol.getServiceInfoList().isEmpty()) {
            log.info("No service on node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
            return;
        }
        rpcProtocolSet.add(rpcProtocol);
        log.info("New service node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
        for (RpcServiceInfo serviceProtocol : rpcProtocol.getServiceInfoList()) {
            log.info("New service info, name: {}, version: {}", serviceProtocol.getServiceName(), serviceProtocol.getVersion());
        }
        final InetSocketAddress remotePeer =
                new InetSocketAddress(rpcProtocol.getHost(), rpcProtocol.getPort());
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                // threadPoolExecutor创建的线程会共用同一个eventLoopGroup，即多个客户端可能会出现并发
                // 注意：同一个eventLoopGroup下的入站、出站管理器是共享的
                b.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new RpcClientInitializer());
                // Netty异步的经典用法，使用ChannelFuture，
                ChannelFuture channelFuture = b.connect(remotePeer);
                // 通过给ChannelFuture添加监听器，当连接完成时，执行operationComplete方法
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            // 如果连接服务器成功，那么得到对应的入站处理器RpcClientHandler
                            log.info("Successfully connect to remote server, remote peer = " + remotePeer);
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            // connectedServerNodeMap 存储已经连接到的服务器节点
                            connectedServerNodeMap.put(rpcProtocol, handler);
                            handler.setRpcProtocol(rpcProtocol);
                            // 多个线程可能会因为使用同一个入站处理器RpcClientHandler而进入阻塞，因此使用下面的方法来通知其他线程此入站处理器可用
                            signalAvailableHandler();
                        } else {
                            log.error("Can not connect to remote server, remote peer = " + remotePeer);
                        }
                    }
                });
            }
        });
    }

    /**
     * 发送信号（通知）给正在等待在 connected 条件上的线程，
     * 告知其他线程此handler可用
     */
    private void signalAvailableHandler() {
        // 进入临界区
        lock.lock();
        try {
            // 唤醒所有正在等待在 connected 条件上的线程
            // 这意味着有多个线程可能在调用 connected.await() 等待在该条件上，一旦调用 signalAll()，它们会被唤醒并继续执行。
            connected.signalAll();
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    /**
     * 移除那些已经过时的Protocol
     * @param rpcProtocol
     */
    private void removeAndCloseHandler(RpcProtocol rpcProtocol) {
        RpcClientHandler handler = connectedServerNodeMap.get(rpcProtocol);
        if (handler != null) {
            handler.close();
        }
        connectedServerNodeMap.remove(rpcProtocol);
        rpcProtocolSet.remove(rpcProtocol);
    }

    public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
        int size = connectedServerNodeMap.values().size();
        while (isRunning && size <= 0) {
            try {
                waitingForHandler();
                size = connectedServerNodeMap.values().size();
            } catch (InterruptedException e) {
                log.error("Waiting for available service is interrupted!", e);
            }
        }
        // route 将不同的服务连接，按服务名分组
        RpcProtocol rpcProtocol = loadBalance.route(serviceKey, connectedServerNodeMap);
        RpcClientHandler handler = connectedServerNodeMap.get(rpcProtocol);
        if (handler != null) {
            return handler;
        } else {
            throw new Exception("Can not get available connection");
        }
    }
    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            log.warn("Waiting for available service");
            return connected.await(this.waitTimeout, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        isRunning = false;
        for (RpcProtocol rpcProtocol : rpcProtocolSet) {
            removeAndCloseHandler(rpcProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}
