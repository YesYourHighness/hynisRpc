package com.hynis.rpc.test.service.impl;

import com.hynis.rpc.common.annotation.RpcService;
import com.hynis.rpc.test.service.HelloService;

/**
 * @author hynis
 * @date 2023/8/2 11:15
 */
@RpcService(value = HelloService.class, version = "1.0")
public class HelloServiceImpl implements HelloService {
    public static final String HELLO_PREFIX = "hello";
    @Override
    public String hello(String name) {
        return HELLO_PREFIX + ":" + name;
    }
}
