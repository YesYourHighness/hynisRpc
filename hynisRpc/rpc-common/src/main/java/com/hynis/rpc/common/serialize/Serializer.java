package com.hynis.rpc.common.serialize;

/**
 * @author hynis
 * @date 2022/3/26 14:49
 * 所有的序列化方式都是
 */
public interface Serializer {

    /**
     * 序列化
     * @param obj
     * @return
     */
    <T> byte[] serialize(T obj);

    /**
     * 反序列化：泛型方法
     * @param bytes 二进制流
     * @param cls 要转换的类型
     * @param <T>
     * @return
     */
    <T> T deserialize(byte[] bytes, Class<T> cls);
}
