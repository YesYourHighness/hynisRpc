package com.hynis.rpc.common.entity;

import com.hynis.rpc.common.enums.RpcResponseEnum;
import lombok.Data;
import lombok.ToString;

/**
 * @author hynis
 * @date 2022/2/23 23:46
 */
@Data
@ToString
public class RpcResponse<T> {
    /**
     * 响应ID
     */
    private String requestId;
    /**
     * 错误代号
     * @see RpcResponseEnum
     */
    private Integer code;
    /**
     * 响应结果
     */
    private String msg;
    /**
     * 数据内容
     */
    private T data;

    /**
     * 请求成功返回模板
     * @param data 成功需要返回的数据
     * @param requestId 请求的ID
     * @param <T> 请求类型
     * @return RpcResponse
     */
    private static <T> RpcResponse <T> success(T data, String requestId){
        RpcResponse<T> rpcResponse = new RpcResponse<T>();
        // 数据设置
        rpcResponse.setRequestId(requestId);
        rpcResponse.setCode(RpcResponseEnum.SUCCESS.getCode());
        rpcResponse.setMsg(RpcResponseEnum.SUCCESS.getMsg());
        rpcResponse.setData(data);

        return rpcResponse;
    }

    /**
     * 请求失败调用模板
     * @param rpcResponseEnum 枚举类
     * @param <T> 泛型
     * @return RpcResponse
     */
    public static <T> RpcResponse<T> fail(RpcResponseEnum rpcResponseEnum) {
        RpcResponse<T> response = new RpcResponse<T>();
        response.setCode(rpcResponseEnum.getCode());
        response.setMsg(rpcResponseEnum.getMsg());
        return response;
    }

}
