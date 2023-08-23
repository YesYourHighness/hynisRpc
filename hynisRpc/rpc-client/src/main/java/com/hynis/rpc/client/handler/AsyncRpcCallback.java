package com.hynis.rpc.client.handler;

/**
 * @author hynis
 * @date 2023/8/19 22:01
 *
 * 异步Rpc回调
 */
public interface AsyncRpcCallback {
    void success(Object result);

    void fail(Exception e);
}
