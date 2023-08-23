package com.hynis.rpc.server.core;

import com.hynis.rpc.common.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * @author hynis
 * @date 2022/2/22 22:08
 */
@Slf4j
public class RpcNettyServer extends NettyServer
        implements InitializingBean, ApplicationContextAware, DisposableBean {

    public RpcNettyServer(String serverAddress, String registerAddress) {
        super(serverAddress, registerAddress);
    }


    /**
     * 初始阶段扫描所有@RpcService注解的接口
     * @param ctx
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RpcService.class);
        if (serviceBeanMap != null && !serviceBeanMap.isEmpty()) {
            for (Object serviceBean : serviceBeanMap.values()) {
                RpcService service = serviceBean.getClass().getAnnotation(RpcService.class);
                String interfaceName = service.value().getName();
                String version = service.version();
                super.addService(interfaceName, version, serviceBean);
            }
        }
    }

    /**
     * 启动服务端
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        super.start();
    }

    /**
     * 销毁服务端
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        super.stop();
    }

}
