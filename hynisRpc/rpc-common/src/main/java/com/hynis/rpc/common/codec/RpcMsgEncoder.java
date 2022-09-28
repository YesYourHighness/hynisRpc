package com.hynis.rpc.common.codec;

import com.hynis.rpc.common.entity.RpcMsg;
import com.hynis.rpc.common.serialize.protostuff.ProtostuffUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author hynis
 * @date 2022/2/23 23:33
 * 编码器
 */
@Slf4j
public class RpcMsgEncoder extends MessageToByteEncoder<RpcMsg> {

    @Autowired
    ProtostuffUtils protostuffUtils;

    /**
     * 将RpcMsg进行编码处理：将RpcMsg转换为容易传输的形式
     * @param ctx 此encode所属的ChannelHandlerContext
     * @param msg 要编码的消息
     * @param out 编码信息将写入的字节码
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMsg msg, ByteBuf out) throws Exception {
        log.info("the msg [{}] is encoding...", msg);
        Object data = msg.getData();
        byte[] serializedData = protostuffUtils.serialize(data);
        out.writeBytes(serializedData);
    }

}
