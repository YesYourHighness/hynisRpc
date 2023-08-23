package com.hynis.rpc.client.route;

import com.hynis.rpc.client.handler.RpcClientHandler;
import com.hynis.rpc.common.protocol.RpcProtocol;
import com.hynis.rpc.common.protocol.RpcServiceInfo;
import com.hynis.rpc.common.util.ServiceUtil;
import org.apache.commons.collections4.map.HashedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author hynis
 * @date 2023/8/19 23:11
 */
public abstract class RpcLoadBalance {
    /**
     * 将所有的服务器节点的服务按其服务名分类，存于serviceMap中返回
     * serviceMap：
     * - key为具体的服务名称
     * - value为RpcProtocol的列表
     * @param connectedServerNodeMap
     * @return
     */
    protected Map<String, List<RpcProtocol>> getServiceMap(Map<RpcProtocol, RpcClientHandler> connectedServerNodeMap) {
        Map<String, List<RpcProtocol>> serviceMap = new HashedMap<>();
        if (connectedServerNodeMap != null && connectedServerNodeMap.size() > 0) {
            // RpcProtocol是一个host:port下的所有服务列表
            for (RpcProtocol rpcProtocol : connectedServerNodeMap.keySet()) {
                // RpcServiceInfo是一个具体的服务，服务名和版本信息
                for (RpcServiceInfo serviceInfo : rpcProtocol.getServiceInfoList()) {
                    String serviceKey = ServiceUtil.makeServiceKey(serviceInfo.getServiceName(), serviceInfo.getVersion());
                    // 如果是
                    List<RpcProtocol> rpcProtocolList = serviceMap.get(serviceKey);
                    if (rpcProtocolList == null) {
                        rpcProtocolList = new ArrayList<>();
                    }
                    rpcProtocolList.add(rpcProtocol);
                    serviceMap.putIfAbsent(serviceKey, rpcProtocolList);
                }
            }
        }
        return serviceMap;
    }

    public abstract RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception;

}
