package com.hynis.rpc.client.core;

import com.hynis.rpc.client.connect.ConnectionManager;
import com.hynis.rpc.client.discovery.ServiceDiscovery;
import com.hynis.rpc.client.proxy.ObjectProxy;
import com.hynis.rpc.client.proxy.RpcService;
import com.hynis.rpc.common.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author hynis
 * @date 2023/8/19 23:04
 */
@Slf4j
public class RpcClient implements ApplicationContextAware, DisposableBean {

    private ServiceDiscovery serviceDiscovery;

    private static ThreadPoolExecutor threadPoolExecutor =
            ThreadPoolUtil.createThreadPool(RpcClient.class.getSimpleName(), 8, 16, 60);


    public RpcClient(String address) {
        this.serviceDiscovery = new ServiceDiscovery(address);
    }

    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }

    @Override
    public void destroy() throws Exception {
        this.stop();
    }

    public void stop() {
        threadPoolExecutor.shutdown();
        serviceDiscovery.stop();
        ConnectionManager.getInstance().stop();
    }

    /**
     * 同步的方式，即简单的使用Proxy代理
     * @param interfaceClass
     * @param version
     * @return
     * @param <T>
     * @param <P>
     */
    public static <T, P> T createService(Class<T> interfaceClass, String version) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T, P>(interfaceClass, version)
        );
    }

    /**
     * 以异步的方式发送请求，异步的实现使用RpcFuture实现
     * @param interfaceClass
     * @param version
     * @return
     * @param <T>
     * @param <P>
     */
    public static <T, P> RpcService createAsyncService(Class<T> interfaceClass, String version) {
        return new ObjectProxy<T, P>(interfaceClass, version);
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        String[] beanNames = applicationContext.getBeanDefinitionNames();
//        for (String beanName : beanNames) {
//            Object bean = applicationContext.getBean(beanName);
//            Field[] fields = bean.getClass().getDeclaredFields();
//            try {
//                for (Field field : fields) {
//                    RpcAutowired rpcAutowired = field.getAnnotation(RpcAutowired.class);
//                    if (rpcAutowired != null) {
//                        String version = rpcAutowired.version();
//                        field.setAccessible(true);
//                        field.set(bean, createService(field.getType(), version));
//                    }
//                }
//            } catch (IllegalAccessException e) {
//                logger.error(e.toString());
//            }
//        }
    }
}
