package com.hynis.rpc.common.serializer;

/**
 * @author hynis
 * @date 2023/8/14 10:17
 *
 * 序列化器抽象类，需要满足序列化与反序列化两种操作
 */
public abstract class Serializer {
    /**
     * 序列化：将对象序列化为字节数组
     * @param obj
     * @return
     * @param <T>
     */
    public abstract <T> byte[] serialize(T obj);

    /**
     * 反序列化：将字节数组转为对象
     * @param bytes
     * @param clazz
     * @return
     * @param <T>
     */
    public abstract <T> Object deserialize(byte[] bytes, Class<T> clazz);
}
