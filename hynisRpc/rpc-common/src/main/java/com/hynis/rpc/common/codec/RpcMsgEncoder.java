package com.hynis.rpc.common.codec;

import com.hynis.rpc.common.entity.RpcMsg;
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
public class RpcMsgEncoder extends MessageToByteEncoder<RpcMsg> {

    /**
     * 要序列化的类型
     */
    private Class<?> genericClass;
    /**
     * 序列化接口：便于扩展序列化工具
     */
    private Serializer serializer;

    /**
     * 将RpcMsg进行编码处理：将RpcMsg转换为容易传输的形式
     * @param ctx 此encode所属的ChannelHandlerContext
     * @param msg 要编码的消息
     * @param out 编码信息将写入的字节码
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMsg msg, ByteBuf out) {
        if(!genericClass.isInstance(msg)){
            log.info("the msg is not the class you want.");
            return;
        }
        log.info("the msg [{}] is encoding...", msg);
        Object data = msg.getData();
        try {
            byte[] serializedData = serializer.serialize(data);
            out.writeBytes(serializedData);
        }catch (Exception e){
            log.info("hynisRpc serialize the msg meets a problem={}", e.getMessage());
        }
    }
}
