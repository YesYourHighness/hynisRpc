package com.hynis.rpc.common.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author hynis
 * @date 2022/2/23 23:50
 *
 *
 */

@Retention(RetentionPolicy.RUNTIME)// 运行时依旧存在，这样才能在运行时被反射获取到
@Target(ElementType.TYPE)// 设置标记的类型: TYPE表示类及接口
@Component// 交由Spring管理此注解
public @interface RpcService {
    /**
     * 服务接口的Class对象
     */
    Class<?> value();

    /**
     * 服务的版本号
      */
    String version() default "";
}
