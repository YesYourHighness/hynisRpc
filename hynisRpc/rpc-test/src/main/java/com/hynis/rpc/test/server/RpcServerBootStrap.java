package com.hynis.rpc.test.server;

import com.hynis.rpc.server.core.RpcNettyServer;
import com.hynis.rpc.test.service.HelloService;
import com.hynis.rpc.test.service.impl.HelloServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hynis
 * @date 2023/8/2 11:53
 */
@Slf4j
public class RpcServerBootStrap {
    public static void main(String[] args) {
        String serverAddress = "127.0.0.1:18877";
        String registryAddress = "192.168.235.151:2181";
        RpcNettyServer rpcServer = new RpcNettyServer(serverAddress, registryAddress);
        HelloService helloService1 = new HelloServiceImpl();
        rpcServer.addService(HelloService.class.getName(), "1.0", helloService1);
        try {
            rpcServer.start();
        } catch (Exception ex) {
            log.error("Exception: {}", ex.toString());
        }
    }
}
