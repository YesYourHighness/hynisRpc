package com.hynis.rpc.client.proxy;

import com.hynis.rpc.client.connect.ConnectionManager;
import com.hynis.rpc.client.handler.RpcClientHandler;
import com.hynis.rpc.client.handler.RpcFuture;
import com.hynis.rpc.common.entity.RpcRequest;
import com.hynis.rpc.common.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @author hynis
 * @date 2023/8/21 11:19
 */
@Slf4j
public class ObjectProxy<T, P> implements InvocationHandler, RpcService<T, P, SerializableFunction<T>> {

    private Class<T> clazz;
    private String version;


    public ObjectProxy(Class<T> interfaceClass, String version) {
        this.clazz = interfaceClass;
        this.version = version;
    }

    /**
     * 实现InvocationHandler接口需要重写invoke方法
     * proxy：代理类
     * method：要代理的方法
     * args：方法所需要的参数
     *
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1、代理Obejct默认方法
        // 检查当前被调用的方法是否是在 Object 类中声明的方法。
        // 如果是，那么说明这是代理对象的一些基本方法，如 equals、hashCode、toString
        if (Object.class == method.getDeclaringClass()) {
            String name = method.getName();
            if ("equals".equals(name)) {
                return proxy == args[0];
            } else if ("hashCode".equals(name)) {
                // 返回proxy的默认哈希码值（返回默认的hashCode方法，无论是否重写过）
                return System.identityHashCode(proxy);
            } else if ("toString".equals(name)) {
                return proxy.getClass().getName() + "@" +
                        Integer.toHexString(System.identityHashCode(proxy)) +
                        ", with InvocationHandler " + this;
            } else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }
        // 2、代理其他方法
        // 封装为 RpcRequest
        RpcRequest request = createRequest(method.getDeclaringClass().getName(), method.getName(), args);

//        RpcRequest request = new RpcRequest();
//        request.setRequestId(UUID.randomUUID().toString());
//        request.setClassName(method.getDeclaringClass().getName());
//        request.setMethodName(method.getName());
//        request.setParameterTypes(method.getParameterTypes());
//        request.setParameters(args);
//        request.setVersion(version);

        // Debug模式
        if (log.isDebugEnabled()) {
            log.debug(method.getDeclaringClass().getName());
            log.debug(method.getName());
            for (int i = 0; i < method.getParameterTypes().length; ++i) {
                log.debug(method.getParameterTypes()[i].getName());
            }
            for (int i = 0; i < args.length; ++i) {
                log.debug(args[i].toString());
            }
        }

        String serviceKey = ServiceUtil.makeServiceKey(method.getDeclaringClass().getName(), version);
        RpcClientHandler handler = ConnectionManager.getInstance().chooseHandler(serviceKey);
        RpcFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture.get();
    }

    @Override
    public RpcFuture call(String funcName, Object... args) throws Exception {
        String serviceKey = ServiceUtil.makeServiceKey(this.clazz.getName(), version);
        RpcClientHandler handler = ConnectionManager.getInstance().chooseHandler(serviceKey);
        RpcRequest request = createRequest(this.clazz.getName(), funcName, args);
        RpcFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture;
    }

    @Override
    public RpcFuture call(SerializableFunction<T> tSerializableFunction, Object... args) throws Exception {
        String serviceKey = ServiceUtil.makeServiceKey(this.clazz.getName(), version);
        RpcClientHandler handler = ConnectionManager.getInstance().chooseHandler(serviceKey);
        RpcRequest request = createRequest(this.clazz.getName(), tSerializableFunction.getName(), args);
        RpcFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture;
    }

    /**
     * 创建RpcRequest对象
     * @param className
     * @param methodName
     * @param args
     * @return
     */

    private RpcRequest createRequest(String className, String methodName, Object[] args) {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(className);
        request.setMethodName(methodName);
        request.setParameters(args);
        request.setVersion(version);
        Class[] parameterTypes = new Class[args.length];
        // Get the right class type
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = getClassType(args[i]);
        }
        request.setParameterTypes(parameterTypes);
        // Debug
        if (log.isDebugEnabled()) {
            log.debug(className);
            log.debug(methodName);
            for (int i = 0; i < parameterTypes.length; ++i) {
                log.debug(parameterTypes[i].getName());
            }
            for (int i = 0; i < args.length; ++i) {
                log.debug(args[i].toString());
            }
        }
        return request;
    }

    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
//        String typeName = classType.getName();
//        switch (typeName) {
//            case "java.lang.Integer":
//                return Integer.TYPE;
//            case "java.lang.Long":
//                return Long.TYPE;
//            case "java.lang.Float":
//                return Float.TYPE;
//            case "java.lang.Double":
//                return Double.TYPE;
//            case "java.lang.Character":
//                return Character.TYPE;
//            case "java.lang.Boolean":
//                return Boolean.TYPE;
//            case "java.lang.Short":
//                return Short.TYPE;
//            case "java.lang.Byte":
//                return Byte.TYPE;
//        }
        return classType;
    }
}
