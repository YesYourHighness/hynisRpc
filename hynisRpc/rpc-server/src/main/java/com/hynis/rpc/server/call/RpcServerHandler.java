package com.hynis.rpc.server.call;

import com.hynis.rpc.common.entity.RpcRequest;
import com.hynis.rpc.common.entity.RpcResponse;
import com.hynis.rpc.common.util.ServiceUtil;
import com.hynis.rpc.common.config.Beat;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.reflect.FastClass;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author hynis
 * @date 2023/8/2 22:22
 *
 * 从request获取反射需要的信息，之后通过动态代理调用对应的方法进行执行，返回response
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    /**
     * key为接口名+版本号，value为对应的实体类
     */
    private final Map<String, Object> serviceMap;
    private final ThreadPoolExecutor serverHandlerPool;

    public RpcServerHandler(Map<String, Object> serviceMap, final ThreadPoolExecutor serverHandlerPool) {
        this.serviceMap = serviceMap;
        this.serverHandlerPool = serverHandlerPool;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx,
                                final RpcRequest rpcRequest) throws Exception {
        // 过滤掉心跳信息
        if (Beat.BEAT_ID.equalsIgnoreCase(rpcRequest.getRequestId())) {
            log.info("Server read heartbeat ping");
            return;
        }
        serverHandlerPool.execute(new Runnable() {
            @Override
            public void run() {
                log.info("Receive request " + rpcRequest.getRequestId());
                RpcResponse response = new RpcResponse();
                response.setRequestId(rpcRequest.getRequestId());
                try {
                    // 将请求传入，动态代理执行方法
                    Object result = handle(rpcRequest);
                    response.setResult(result);
                } catch (Throwable t) {
                    response.setError(t.toString());
                    log.error("RPC Server handle request error", t);
                }
                ctx.writeAndFlush(response)
                        .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        log.info("Send response for request " + rpcRequest.getRequestId());
                    }
                });
            }
        });
    }

    /**
     * 处理请求
     * @param rpcRequest
     * @return
     */
    private Object handle(RpcRequest rpcRequest) throws InvocationTargetException {
        String className = rpcRequest.getClassName();
        String version = rpcRequest.getVersion();

        String serviceKey = ServiceUtil.makeServiceKey(className, version);
        Object serviceBean = serviceMap.get(serviceKey);

        if (serviceBean == null) {
            log.error("Can not find service implement with interface name: {} and version: {}", className, version);
            return null;
        }


        Class<?> serviceClass = serviceBean.getClass();
        String methodName = rpcRequest.getMethodName();
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        Object[] parameters = rpcRequest.getParameters();

        log.debug(serviceClass.getName());
        log.debug(methodName);
        for (int i = 0; i < parameterTypes.length; ++i) {
            log.debug(parameterTypes[i].getName());
        }
        for (int i = 0; i < parameters.length; ++i) {
            log.debug(parameters[i].toString());
        }
        // 利用反射创建对象：可选择使用JDK动态代理或是CGlib动态代理
        // 1、JDK reflect
//        Method method = serviceClass.getMethod(methodName, parameterTypes);
//        method.setAccessible(true);
//        return method.invoke(serviceBean, parameters);

        // 2、Cglib reflect
        // 创建代理类
        FastClass serviceFastClass = FastClass.create(serviceClass);
        /* 使用getMethod也是可以的
        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
        return serviceFastMethod.invoke(serviceBean, parameters);
         */
        // 使用getIndex方法相比于getMethod可以提高性能：它会对被代理类的方法进行索引和缓存，从而避免了每次方法调用都要通过反射来定位方法
        // getIndex方法参数有两个：methodName: 要获取索引的方法的名称；parameterTypes: 方法的参数类型数组。
        int methodIndex = serviceFastClass.getIndex(methodName, parameterTypes);
        // 反射调用对应类的方法，传入方法索引、类实现、参数
        return serviceFastClass.invoke(methodIndex, serviceBean, parameters);
    }

    /**
     * 网络通信的时候，当出现异常情况时会被调用
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("Server caught exception: " + cause.getMessage());
        ctx.close();
    }

    /**
     * 重写此方法可以处理自定义事件：此处处理逻辑为，如果为IdleStateEvent的实例，
     * 则证明一段时间内没有接收到输入或输出请求，就可以关闭通道
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
            log.warn("Channel idle in last {} seconds, close it", Beat.BEAT_TIMEOUT);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
