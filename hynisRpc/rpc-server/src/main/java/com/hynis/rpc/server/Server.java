package com.hynis.rpc.server;

/**
 * @author hynis
 * @date 2022/2/22 21:09
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
