package com.hynis.rpc.common.codec;

import com.hynis.rpc.common.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hynis
 * @date 2022/2/23 23:33
 * 编码器
 */
@Slf4j
public class RpcMsgEncoder extends MessageToByteEncoder<Object> {
    /**
     * 要序列化的类型
     */
    private Class<?> genericClass;
    /**
     * 序列化接口：便于扩展序列化工具
     */
    private Serializer serializer;

    public RpcMsgEncoder(Class<?> genericClass, Serializer serializer){
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    /**
     * 将RpcMsg进行编码处理：将RpcMsg转换为容易传输的形式
     * @param ctx 此encode所属的ChannelHandlerContext
     * @param data 要编码的消息
     * @param out 编码信息将写入的字节码
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Object data, ByteBuf out) {
        if(genericClass != null && !genericClass.isInstance(data)){
            log.info("the msg is not the class you want.");
            return;
        }
        log.info("the msg [{}] is encoding...", data);
        try {
            byte[] serializedData = serializer.serialize(data);
            out.writeBytes(serializedData);
        }catch (Exception e){
            log.info("hynisRpc serialize the msg meets a problem={}", e.getMessage());
        }
    }
}
