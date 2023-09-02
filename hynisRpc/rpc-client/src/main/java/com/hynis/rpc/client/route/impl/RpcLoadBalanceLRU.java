package com.hynis.rpc.client.route.impl;

import com.hynis.rpc.client.handler.RpcClientHandler;
import com.hynis.rpc.client.route.RpcLoadBalance;
import com.hynis.rpc.common.protocol.RpcProtocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author hynis
 * @date 2023/9/2 22:12
 *
 * LRU算法
 */
public class RpcLoadBalanceLRU extends RpcLoadBalance {
    private ConcurrentMap<String, LinkedHashMap<RpcProtocol, RpcProtocol>> jobLruMap = new ConcurrentHashMap<String, LinkedHashMap<RpcProtocol, RpcProtocol>>();
    private long CACHE_VALID_TIME = 0;

    public RpcProtocol doRoute(String serviceKey, List<RpcProtocol> addressList) {
        // 缓存时间，如果缓存时间到期重置map
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            jobLruMap.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }

        // LRU算法实现：
        // LinkedHashMap是一个链表实现的HashMap，与HashMap不同，它的性能较低
        LinkedHashMap<RpcProtocol, RpcProtocol> lruHashMap = jobLruMap.get(serviceKey);
        if (lruHashMap == null) {
            /**
             * LinkedHashMap
             * a、accessOrder 设置为 true，则 LinkedHashMap 会按照访问顺序来维护键值对，即最近访问的元素会被放在最后。；
             * b、removeEldestEntry：新增元素时将会调用，返回true时会删除最老元素；默认返回 false，表示不会自动删除最旧的条目
             */
            lruHashMap = new LinkedHashMap<RpcProtocol, RpcProtocol>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<RpcProtocol, RpcProtocol> eldest) {
                    // 元素数量超过容量限制1000时自动删除最旧的条目
                    if (super.size() > 1000) {
                        return true;
                    } else {
                        return false;
                    }
                }
            };
            jobLruMap.putIfAbsent(serviceKey, lruHashMap);
        }

        // put new
        for (RpcProtocol address : addressList) {
            if (!lruHashMap.containsKey(address)) {
                lruHashMap.put(address, address);
            }
        }
        // remove old
        List<RpcProtocol> delKeys = new ArrayList<>();
        for (RpcProtocol existKey : lruHashMap.keySet()) {
            if (!addressList.contains(existKey)) {
                delKeys.add(existKey);
            }
        }
        if (delKeys.size() > 0) {
            for (RpcProtocol delKey : delKeys) {
                lruHashMap.remove(delKey);
            }
        }

        // load
        RpcProtocol eldestKey = lruHashMap.entrySet().iterator().next().getKey();
        RpcProtocol eldestValue = lruHashMap.get(eldestKey);
        return eldestValue;
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
