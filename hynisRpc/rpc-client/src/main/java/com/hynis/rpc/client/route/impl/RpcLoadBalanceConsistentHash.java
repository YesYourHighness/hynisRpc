package com.hynis.rpc.client.route.impl;

import com.google.common.hash.Hashing;
import com.hynis.rpc.client.handler.RpcClientHandler;
import com.hynis.rpc.client.route.RpcLoadBalance;
import com.hynis.rpc.common.protocol.RpcProtocol;

import java.util.List;
import java.util.Map;

/**
 * @author hynis
 * @date 2023/9/2 22:10
 *
 * 使用一致性哈希算法，Google提供了实现包
 */
public class RpcLoadBalanceConsistentHash extends RpcLoadBalance {
    public RpcProtocol doRoute(String serviceKey, List<RpcProtocol> addressList) {
        int index = Hashing.consistentHash(serviceKey.hashCode(), addressList.size());
        return addressList.get(index);
    }

    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcProtocol> addressList = serviceMap.get(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(serviceKey, addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
