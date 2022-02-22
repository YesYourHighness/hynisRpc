package com.hynis.rpc.server;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author hynis
 * @date 2022/2/22 22:08
 */
public class RpcNettyServer extends NettyServer
        implements InitializingBean, ApplicationContextAware, DisposableBean {

    /**
     * 注册服务
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
