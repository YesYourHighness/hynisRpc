package com.hynis.rpc.server.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author hynis
 * @date 2022/2/22 22:08
 */
@Slf4j
public class RpcNettyServer extends NettyServer
        implements InitializingBean, ApplicationContextAware, DisposableBean {

    /**
     * 初始阶段扫描所有@RpcService注解的接口
     * @param applicationContext
     * @throws BeansException
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }

    /**
     * 启动服务端
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        super.start();
    }

    /**
     * 销毁服务端
     * @throws Exception
     */
    public void destroy() throws Exception {
        super.stop();
    }
}
