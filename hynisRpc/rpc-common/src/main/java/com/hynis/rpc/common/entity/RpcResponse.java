package com.hynis.rpc.common.entity;

import lombok.Data;

/**
 * @author hynis
 * @date 2022/2/23 23:46
 *
 * 响应方法
 */
@Data
public class RpcResponse {
    /**
     * 对应的requestID
     */
    private String requestId;
    /**
     * 如果代理执行方法时出错，此值将不为null
     */
    private String error;
    private Object result;

    public boolean isError() {
        return error != null;
    }
}
