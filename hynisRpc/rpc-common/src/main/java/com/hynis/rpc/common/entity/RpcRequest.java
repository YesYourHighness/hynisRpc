package com.hynis.rpc.common.entity;

import lombok.Data;

/**
 * @author hynis
 * @date 2022/2/23 23:46
 */
@Data
public class RpcRequest {
    private String requestId;
    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
    private String version;
}
