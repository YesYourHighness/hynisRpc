package com.hynis.rpc.client.proxy;

import com.hynis.rpc.client.handler.RpcFuture;

/**
 * @author hynis
 * @date 2023/8/21 17:12
 */
public interface RpcService<T, P, FN extends SerializableFunction<T>> {

    /**
     * 调用Rpc服务
     * @param funcName 方法名称
     * @param args 参数
     * @return RpcFuture
     * @throws Exception
     */
    RpcFuture call(String funcName, Object... args) throws Exception;

    /**
     * 重载方法
     * @param fn 一个实现了SerializableFunction接口的函数，可以被序列化
     * @param args 参数
     * @return
     * @throws Exception
     */
    RpcFuture call(FN fn, Object... args) throws Exception;
}
