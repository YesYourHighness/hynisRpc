package com.hynis.rpc.common.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;

import java.util.List;

/**
 * @author hynis
 * @date 2023/8/3 10:45
 * Curator是一个优秀的ZooKeeper客户端框架，它封装了ZooKeeper客户端的复杂性
 * 提供了一组易于使用的高级API
 * 方便开发者与ZooKeeper进行交互
 */
public class CuratorClient {
    private CuratorFramework client;

    /**
     * Curator构造方法
     * @param connectString Zookeeper服务器连接地址，例如 localhost:2181
     * @param namespace Zookeeper命名空间
     * @param sessionTimeout 会话超时时间，默认为ms
     * @param connectionTimeout 连接超时时间，默认为ms
     */
    public CuratorClient(String connectString, String namespace, int sessionTimeout, int connectionTimeout) {
        client = CuratorFrameworkFactory
                .builder()
                .namespace(namespace)
                .connectString(connectString)
                .sessionTimeoutMs(sessionTimeout)
                .connectionTimeoutMs(connectionTimeout)
                // retryPolicy重试策略：表示指数退避重试策略。该策略会在连接失败后，隔一段时间后进行重试，重试次数为10次。
                .retryPolicy(new ExponentialBackoffRetry(1000, 10))
                .build();
        client.start();
    }

    public CuratorClient(String connectString, int timeout) {
        this(connectString, ZookeeperConstant.ZK_NAMESPACE, timeout, timeout);
    }

    public CuratorClient(String connectString) {
        this(connectString, ZookeeperConstant.ZK_NAMESPACE, ZookeeperConstant.ZK_SESSION_TIMEOUT, ZookeeperConstant.ZK_CONNECTION_TIMEOUT);
    }

    /** 下面是Zookeeper相关API */
    public CuratorFramework getClient() {
        return client;
    }

    /**
     * 向CuratorFramework添加连接状态监听器
     * 连接状态可能会发生变化，例如连接成功、连接断开、重新连接等
     * @param connectionStateListener 状态监听器
     */
    public void addConnectionStateListener(ConnectionStateListener connectionStateListener) {
        client.getConnectionStateListenable().addListener(connectionStateListener);
    }

    /**
     * 用于在ZooKeeper中创建一个节点node，并设置节点的数据
     * @param path
     * @param data
     * @return 返回创建后的node的完整路径
     * @throws Exception
     */
    public String createPathData(String path, byte[] data) throws Exception {
        return client.create()
                // creatingParentsIfNeeded：如果父节点不存在，会自动创建父节点
                .creatingParentsIfNeeded()
                // EPHEMERAL_SEQUENTIAL：表示创建一个临时顺序节点。临时节点会在客户端会话结束后自动删除，并且带有顺序号
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(path, data);
    }

    public void updatePathData(String path, byte[] data) throws Exception {
        client.setData().forPath(path, data);
    }

    public void deletePath(String path) throws Exception {
        client.delete().forPath(path);
    }

    /**
     * 在ZooKeeper中监视指定节点的数据变化
     * @param path
     * @param watcher Watcher是ZooKeeper中的一个机制，用于监听节点数据的变化、节点的创建和删除等事件，当节点的数据发生变化时，ZooKeeper会通知客户端的Watcher对象
     * @throws Exception
     */
    public void watchNode(String path, Watcher watcher) throws Exception {
        client.getData().usingWatcher(watcher).forPath(path);
    }

    public byte[] getData(String path) throws Exception {
        return client.getData().forPath(path);
    }

    public List<String> getChildren(String path) throws Exception {
        return client.getChildren().forPath(path);
    }

    /**
     * 监视指定节点及其子节点
     * @param path
     * @param listener Curator库中提供的用于监视节点及其子节点的类
     */
    public void watchTreeNode(String path, TreeCacheListener listener) {
        // TreeCache Curator库中提供的用于监视节点及其子节点的类
        TreeCache treeCache = new TreeCache(client, path);
        treeCache.getListenable().addListener(listener);
    }

    /**
     * 用于在ZooKeeper中监视指定节点的子节点的变化
     * @param path
     * @param listener
     * @throws Exception
     */
    public void watchPathChildrenNode(String path, PathChildrenCacheListener listener) throws Exception {
        PathChildrenCache pathChildrenCache = new PathChildrenCache(client, path, true);
        // 启动PathChildrenCache，并且使用同步的方式进行缓存初始化 BUILD_INITIAL_CACHE
        pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        pathChildrenCache.getListenable().addListener(listener);
    }

    public void close() {
        client.close();
    }
}
