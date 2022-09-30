package com.hynis.rpc.common.enums;

import lombok.Getter;

/**
 * @author hynis
 * @date 2022/9/30 16:39
 */
@Getter
public enum RpcResponseEnum {
    /**
     * 成功 返回值 200
     */
    SUCCESS(200, "RPC is successful"),
    /**
     * 失败 返回值 500
     */
    FAIL(500, "RPC is fail");

    /**
     * 返回值
     */
    private final Integer code;
    /**
     * 消息
     */
    private final String msg;

    RpcResponseEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
