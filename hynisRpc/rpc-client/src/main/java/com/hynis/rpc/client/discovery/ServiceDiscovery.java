package com.hynis.rpc.client.discovery;

import com.hynis.rpc.client.connect.ConnectionManager;
import com.hynis.rpc.common.protocol.RpcProtocol;
import com.hynis.rpc.common.zookeeper.CuratorClient;
import com.hynis.rpc.common.zookeeper.ZookeeperConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hynis
 * @date 2023/8/16 17:24
 *
 * 服务发现
 */
@Slf4j
public class ServiceDiscovery {
    private CuratorClient curatorClient;

    public ServiceDiscovery(String registryAddress) {
        this.curatorClient = new CuratorClient(registryAddress);
        discoveryService();
    }

    /**
     * 服务发现：初始化扫描服务，并且添加监听器，一旦服务状态发生改变，就更新服务
     */
    private void discoveryService() {
        try {
            // 获得Zookeeper上的初始服务
            log.info("Get initial service info");
            getServiceAndUpdateServer();
            // 添加监听器，一旦服务状态发生变化就更新
            curatorClient.watchPathChildrenNode(ZookeeperConstant.ZK_REGISTRY_PATH, new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                    // 获取事件的类型与数据，分情况进行处理
                    PathChildrenCacheEvent.Type type = pathChildrenCacheEvent.getType();
                    ChildData childData = pathChildrenCacheEvent.getData();
                    switch (type) {
                        case CONNECTION_RECONNECTED:
                            log.info("Reconnected to zk, try to get latest service list");
                            getServiceAndUpdateServer();
                            break;
                        case CHILD_ADDED:
                            getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_ADDED);
                            break;
                        case CHILD_UPDATED:
                            getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_UPDATED);
                            break;
                        case CHILD_REMOVED:
                            getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_REMOVED);
                            break;
                    }
                }
            });
        } catch (Exception ex) {
            log.error("Watch node exception: " + ex.getMessage());
        }

    }

    /**
     * curatorClient读取节点数据，然后反序列化为RpcProtocol
     */
    private void getServiceAndUpdateServer() {
        try {
            List<String> nodeList = curatorClient.getChildren(ZookeeperConstant.ZK_REGISTRY_PATH);
            List<RpcProtocol> dataList = new ArrayList<>();
            for (String node : nodeList) {
                log.debug("Service node: " + node);
                byte[] bytes = curatorClient.getData(ZookeeperConstant.ZK_REGISTRY_PATH + "/" + node);
                String json = new String(bytes);
                RpcProtocol rpcProtocol = RpcProtocol.fromJson(json);
                dataList.add(rpcProtocol);
            }
            log.debug("Service node data: {}", dataList);
            //更新Zookeeper的服务信息到本地
            UpdateConnectedServer(dataList);
        } catch (Exception e) {
            log.error("Get node exception: " + e.getMessage());
        }
    }

    /**
     * 获取对应状态的节点，更新这些状态变化的节点
     * @param childData
     * @param type
     */
    private void getServiceAndUpdateServer(ChildData childData, PathChildrenCacheEvent.Type type) {
        String path = childData.getPath();
        String data = new String(childData.getData(), StandardCharsets.UTF_8);
        log.info("Child data updated, path:{},type:{},data:{},", path, type, data);
        RpcProtocol rpcProtocol =  RpcProtocol.fromJson(data);
        updateConnectedServer(rpcProtocol, type);
    }

    /**
     * 更新Zookeeper端的服务到本地
     * @param dataList
     */
    private void UpdateConnectedServer(List<RpcProtocol> dataList) {
        ConnectionManager.getInstance().updateConnectedServer(dataList);
    }

    /**
     * 更新Zookeeper端的服务到本地
     * @param rpcProtocol
     * @param type
     */
    private void updateConnectedServer(RpcProtocol rpcProtocol, PathChildrenCacheEvent.Type type) {
        ConnectionManager.getInstance().updateConnectedServer(rpcProtocol, type);
    }

    public void stop() {
        this.curatorClient.close();
    }
}
