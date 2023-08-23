package com.hynis.rpc.client.handler;

import com.hynis.rpc.common.entity.RpcRequest;
import com.hynis.rpc.common.entity.RpcResponse;
import com.hynis.rpc.common.protocol.RpcProtocol;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hynis
 * @date 2023/8/17 20:21
 */
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private ConcurrentHashMap<String, RpcFuture> pendingRpcMap = new ConcurrentHashMap<>();
    private RpcProtocol rpcProtocol;
    private SocketAddress remotePeer;

    private volatile Channel channel;

    /**
     * SimpleChannelInboundHandler各个方法的执行顺序为：
     * 1、channelRegistered 通道被注册到EventLoop时调用
     * 2、channelActive 通道变为活跃状态时调用，通常为通道成功连接
     * 3、channelRead0 核心方法，当有入站消息可读取时调用
     * 4、channelReadComplete 一批消息读取完成后调用，用来执行一些批量处理的逻辑，例如刷新缓冲区或执行其他收尾操作
     * 5、channelInactive 通道变为非活跃状态时调用
     * 6、exceptionCaught 处理过程中出现异常时调用
     * @param ctx
     * @throws Exception
     */

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remotePeer = this.channel.remoteAddress();
    }

    /**
     * 将请求封装为RPCFuture
     * pendingRpcMap存放已经发送出去的异步请求，以便后续使用id查询状态
     * @param request
     * @return
     */
    public RpcFuture sendRequest(RpcRequest request) {
        RpcFuture rpcFuture = new RpcFuture(request);
        pendingRpcMap.put(request.getRequestId(), rpcFuture);
        try {
            ChannelFuture channelFuture = channel.writeAndFlush(request).sync();
            if (!channelFuture.isSuccess()) {
                log.error("Send request {} error", request.getRequestId());
            }
        } catch (InterruptedException e) {
            log.error("Send request exception: " + e.getMessage());
        }

        return rpcFuture;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse response) throws Exception {
        String requestId = response.getRequestId();
        log.debug("Receive response: " + requestId);
        RpcFuture rpcFuture = pendingRpcMap.get(requestId);
        if (rpcFuture != null) {
            pendingRpcMap.remove(requestId);
            rpcFuture.done(response);
        } else {
            log.warn("Can not get pending response for request id: " + requestId);
        }
    }

    public void setRpcProtocol(RpcProtocol rpcProtocol) {
        this.rpcProtocol = rpcProtocol;
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
