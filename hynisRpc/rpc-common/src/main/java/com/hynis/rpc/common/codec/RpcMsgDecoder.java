package com.hynis.rpc.common.codec;

import com.hynis.rpc.common.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author hynis
 * @date 2022/2/23 23:33
 * 解码器
 */
@Slf4j
public class RpcMsgDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;
    private Serializer serializer;

    public RpcMsgDecoder(Class<?> genericClass, Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        // 小于一个int（即4个字节）将不再读取
        if (in.readableBytes() < 4) {
            return;
        }
        // 标记当前读的位置
        in.markReaderIndex();
        // 读一个int，这个int标记了长度
        int dataLength = in.readInt();
        // 检查剩余的可读字节数是否足够读取完整的数据，如果不足则回退到之前标记的读取位置并返回
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        Object obj = null;
        try {
            obj = serializer.deserialize(data, genericClass);
            out.add(obj);
        } catch (Exception ex) {
            log.error("Decode error: " + ex.toString());
        }
    }
}
