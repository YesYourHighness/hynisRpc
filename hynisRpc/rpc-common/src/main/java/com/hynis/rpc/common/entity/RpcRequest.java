package com.hynis.rpc.common.entity;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * @author hynis
 * @date 2022/2/23 23:46
 */
@Data
@ToString
@Builder
public class RpcRequest {
    /**
     * 请求ID：用UUID生成
     */
    private String requestId;
    /**
     * 接口名
     */
    private String interfaceName;
    /**
     * 方法名
     */
    private String methodName;
    /**
     * 参数
     */
    private Object[] parameters;
    /**
     * 参数类型
     */
    private Class<?>[] paramTypes;
    /**
     * 版本号
     */
    private String version;
    /**
     * 分组
     */
    private String group;

    /**
     *
     * @return 获取服务名：接口名+组+版本 唯一确定一个服务
     */
    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}
