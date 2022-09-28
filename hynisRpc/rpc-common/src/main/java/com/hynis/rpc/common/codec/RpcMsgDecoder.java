package com.hynis.rpc.common.codec;

import com.hynis.rpc.common.entity.RpcMsg;
import com.hynis.rpc.common.serialize.Serializer;
import com.hynis.rpc.common.serialize.protostuff.ProtostuffUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author hynis
 * @date 2022/2/23 23:33
 * 解码器
 */
@Slf4j
public class RpcMsgDecoder extends ByteToMessageDecoder {

    /**
     * 要序列化的类型
     */
    private Class<?> genericClass;
    /**
     * 序列化接口
     */
    private Serializer serializer;

    public RpcMsgDecoder(Class<?> genericClass, Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    /**
     * @param channelHandlerContext 所属的channelHandlerContext
     * @param byteBuf               读取的字节流
     * @param list                  读取后存入list
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext,
                          ByteBuf byteBuf, List<Object> list) throws Exception {
        // 当可读取字节至少为4，才去读取（如果逻辑复杂可以直接继承类ReplayingDecoder）
        if (byteBuf.readableBytes() < 4) {
            return;
        }
        // 标记当前的读位置
        byteBuf.markReaderIndex();
        // readInt读取4个字节，这将会移动读指针4个位置
        int dataLength = byteBuf.readInt();
        // 可读部分字节数小于读取的到的字节数，那么就等待重新读取
        if (byteBuf.readableBytes() < dataLength) {
            // 重置读位置到标记的位置
            byteBuf.resetReaderIndex();
            return;
        }
        // 存储要读取的数据
        byte[] data = new byte[dataLength];
        // 将数据读取到data数组内
        byteBuf.readBytes(data);
        Object obj = null;
        try {
            // 将数据转为所要的类型
            obj = serializer.deserialize(data, genericClass);
            list.add(obj);
        } catch (Exception e) {
            // 说明反序列化遇到问题
            log.info("hynisRpc deserialize has a problem={}", e.getMessage());
        }
    }
}
