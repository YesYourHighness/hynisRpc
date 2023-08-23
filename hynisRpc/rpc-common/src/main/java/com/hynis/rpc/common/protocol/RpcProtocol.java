package com.hynis.rpc.common.protocol;

import com.hynis.rpc.common.util.JsonUtil;
import lombok.Data;

import java.util.List;

/**
 * @author hynis
 * @date 2023/8/3 15:50
 * Rpc协议：主机号、端口、支持的服务列表
 */
@Data
public class RpcProtocol {
    private String host;
    private int port;
    private List<RpcServiceInfo> serviceInfoList;

    public String toJson() {
        return JsonUtil.objectToJson(this);
    }

    public static RpcProtocol fromJson(String json) {
        return JsonUtil.jsonToObject(json, RpcProtocol.class);
    }

}
