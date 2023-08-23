package com.hynis.rpc.common.protocol;

import com.hynis.rpc.common.util.JsonUtil;
import lombok.Data;

import java.io.Serializable;

/**
 * @author hynis
 * @date 2023/8/3 11:36
 * Rpc服务信息：服务名、版本信息
 */
@Data
public class RpcServiceInfo implements Serializable {
    private String serviceName;
    private String version;

    /**
     * 使用Jackson进行序列化
     * @return
     */
    public String toJson() {
        return JsonUtil.objectToJson(this);
    }

    @Override
    public String toString() {
        return toJson();
    }
}
