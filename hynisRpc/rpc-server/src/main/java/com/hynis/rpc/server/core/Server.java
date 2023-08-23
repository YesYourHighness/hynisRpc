package com.hynis.rpc.server.core;

/**
 * @author hynis
 * @date 2022/2/22 21:09
 *
 * Server端开启与关闭
 */
public abstract class Server {
    /**
     * 启动Server
     */
    public abstract void start();

    /**
     * 停止Server
     */
    public abstract void stop();
}
