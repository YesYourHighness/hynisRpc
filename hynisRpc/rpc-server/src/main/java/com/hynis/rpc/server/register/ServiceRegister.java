package com.hynis.rpc.server.register;

import com.hynis.rpc.common.protocol.RpcProtocol;
import com.hynis.rpc.common.protocol.RpcServiceInfo;
import com.hynis.rpc.common.util.ServiceUtil;
import com.hynis.rpc.common.zookeeper.ZookeeperConstant;
import com.hynis.rpc.common.zookeeper.CuratorClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author hynis
 * @date 2023/8/3 10:43
 *
 * 服务注册实现两个功能：服务发现、服务注册、服务取消注册
 */
@Slf4j
public class ServiceRegister {
    private CuratorClient curatorClient;
    private List<String> pathList = new ArrayList<>();

    public ServiceRegister(String registryAddress) {
        this.curatorClient = new CuratorClient(registryAddress, 5000);
    }

    /**
     * 服务注册：服务端会给出自己提供的服务信息RpcServiceInfo，存于serviceMap。
     * 本方法将服务封装在RpcProtocol类内，调用Curator将数据提交给Zookeeper，以便于用户发现服务去对应的host与port调用
     * Zookeeper会对外展示支持的功能
     * @param host
     * @param port
     * @param serviceMap
     */
    public void registerService(final String host, final int port, final Map<String, Object> serviceMap) {
        // 服务注册
        List<RpcServiceInfo> serviceInfoList = new ArrayList<>();
        for (String key : serviceMap.keySet()) {
            String[] serviceInfo = key.split(ServiceUtil.SERVICE_CONCAT_TOKEN);
            if (serviceInfo.length > 0) {
                RpcServiceInfo rpcServiceInfo = new RpcServiceInfo();
                rpcServiceInfo.setServiceName(serviceInfo[0]);
                if (serviceInfo.length == 2) {
                    rpcServiceInfo.setVersion(serviceInfo[1]);
                } else {
                    rpcServiceInfo.setVersion("");
                }
                log.info("Register new service: {} ", key);
                serviceInfoList.add(rpcServiceInfo);
            } else {
                log.warn("Can not get service name and version: {} ", key);
            }
        }
        try {
            RpcProtocol rpcProtocol = new RpcProtocol();
            rpcProtocol.setHost(host);
            rpcProtocol.setPort(port);
            rpcProtocol.setServiceInfoList(serviceInfoList);
            String serviceData = rpcProtocol.toJson();
            byte[] bytes = serviceData.getBytes();
            String path = ZookeeperConstant.ZK_DATA_PATH + "-" + rpcProtocol.hashCode();
            path = this.curatorClient.createPathData(path, bytes);
            pathList.add(path);
            log.info("Register {} new service, host: {}, port: {}", serviceInfoList.size(), host, port);
        } catch (Exception e) {
            log.error("Register service fail, exception: {}", e.getMessage());
        }

        // 给新创建的Zookeeper节点添加监听器
        curatorClient.addConnectionStateListener(new ConnectionStateListener() {
            // 一旦对应节点的连接状态重新连接，那么就重新进行注册服务
            @Override
            public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                if (connectionState == ConnectionState.RECONNECTED) {
                    log.info("Connection state: {}, register service after reconnected", connectionState);
                    registerService(host, port, serviceMap);
                }
            }
        });
    }

    /**
     * 取消注册所有的服务
     */
    public void unregisterService() {
        log.info("Unregister all service");
        for (String path : pathList) {
            try {
                this.curatorClient.deletePath(path);
            } catch (Exception ex) {
                log.error("Delete service path error: " + ex.getMessage());
            }
        }
        this.curatorClient.close();
    }

}
