package com.hynis.rpc.common.entity;

import lombok.Data;

/**
 * @author hynis
 * @date 2022/2/23 23:37
 */
@Data
public class RpcService {
    /**
     * 服务名
     */
    private String serviceName;
    private String group;
    private String version;
}
