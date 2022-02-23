package com.hynis.rpc.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author hynis
 * @date 2022/2/23 23:50
 */
// 运行时依旧存在，这样才能在运行时被反射获取到
@Retention(RetentionPolicy.RUNTIME)
// 设置标记的类型
@Target(ElementType.TYPE)
public @interface RpcService {
}
