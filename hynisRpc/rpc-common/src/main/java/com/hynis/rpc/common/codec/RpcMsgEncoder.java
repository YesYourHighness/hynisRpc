package com.hynis.rpc.common.codec;

import com.hynis.rpc.common.serializer.Serializer;
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
public class RpcMsgEncoder extends MessageToByteEncoder {

    /**
     * 要序列化的对象类型
     */
    private Class<?> genericClass;
    /**
     * 要使用的序列化器
     */
    private Serializer serializer;

    public RpcMsgEncoder(Class<?> genericClass, Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    /**
     * 编码
     * @param channelHandlerContext
     * @param in
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)) {
            try {
                byte[] data = serializer.serialize(in);
                out.writeInt(data.length);
                out.writeBytes(data);
            } catch (Exception ex) {
                log.error("Encode error: " + ex.toString());
            }
        }
    }
}
