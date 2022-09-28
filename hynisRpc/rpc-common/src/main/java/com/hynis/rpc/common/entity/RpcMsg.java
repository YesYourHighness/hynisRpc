package com.hynis.rpc.common.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hynis
 * @date 2022/3/26 11:18
 */
@Data
@NoArgsConstructor
public class RpcMsg {
    /**
     * 消息类型
     */
    private byte msgType;
    /**
     * 序列化类型
     */
    private byte codec;
    /**
     * 压缩类型
     */
    private byte compress;
    /**
     * 请求ID
     */
    private int requestId;
    /**
     * 请求数据
     */
    private Object data;
}
