package com.hynis.rpc.client.proxy;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/**
 * @author hynis
 * @date 2023/8/21 17:01
 *
 * 序列化方法接口，任何实现此接口的都可以被序列化
 */
public interface SerializableFunction<T> extends Serializable {

    /**
     * 可以从 实现了SerializableFunction 接口的类中获取一个特定方法的名称
     * @return
     * @throws Exception
     */
    default String getName() throws Exception {
        // 当一个对象被序列化时，Java 的序列化机制会检查该对象是否有一个名为 writeReplace 的私有方法。
        // 如果存在这个方法，那么在实际序列化之前，会首先调用这个方法，然后将 writeReplace 方法返回的对象进行序列化。
        // 这样可以实现一种在序列化时替换原始对象的机制。
        Method write = this.getClass().getDeclaredMethod("writeReplace");
        // 允许访问那些private方法
        write.setAccessible(true);
        // 通过反射调用了之前获取的 writeReplace 方法，并将当前对象作为参数传递给它。
        // 由于 writeReplace 方法返回一个 SerializedLambda 对象，因此我们将其强制转换为 SerializedLambda 类型
        SerializedLambda serializedLambda = (SerializedLambda) write.invoke(this);
        return serializedLambda.getImplMethodName();
    }
}
