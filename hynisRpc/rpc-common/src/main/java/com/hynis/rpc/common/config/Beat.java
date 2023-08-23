package com.hynis.rpc.common.config;

import com.hynis.rpc.common.entity.RpcRequest;

/**
 * @author hynis
 * @date 2023/8/2 18:21
 */

public class Beat {
    /**
     * 心跳间隔：30s
     */
    public static final int BEAT_INTERVAL = 30;
    /**
     * 心跳超时：3倍的心跳间隔
    */
    public static final int BEAT_TIMEOUT = 3 * BEAT_INTERVAL;
    /**
     * 心跳发送的请求ID，标识此请求是一个心跳请求
     */
    public static final String BEAT_ID = "BEAT_PING_PONG";

    public static RpcRequest BEAT_PING;

    static {
        BEAT_PING = new RpcRequest();
        BEAT_PING.setRequestId(BEAT_ID);
    }
}
