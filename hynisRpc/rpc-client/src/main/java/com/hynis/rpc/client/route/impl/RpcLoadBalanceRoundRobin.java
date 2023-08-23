package com.hynis.rpc.client.route.impl;

import com.hynis.rpc.client.handler.RpcClientHandler;
import com.hynis.rpc.client.route.RpcLoadBalance;
import com.hynis.rpc.common.protocol.RpcProtocol;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hynis
 * @date 2023/8/19 23:12
 *
 * "Round Robin" 是一种简单的负载均衡策略，循环发放
 */
public class RpcLoadBalanceRoundRobin extends RpcLoadBalance {

    private AtomicInteger roundRobin = new AtomicInteger(0);

    /**
     *
     * @param serviceKey
     * @param connectedServerNodeMap
     * @return
     * @throws Exception
     */
    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodeMap) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodeMap);
        List<RpcProtocol> addressList = serviceMap.get(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }

    /**
     * 从给定的服务器节点列表中选择一个节点：循环选取
     * @param addressList
     * @return
     */
    public RpcProtocol doRoute(List<RpcProtocol> addressList) {
        // 获取节点服务器的个数
        int size = addressList.size();
        int index = (roundRobin.getAndAdd(1) + size) % size;
        return addressList.get(index);
    }
}
