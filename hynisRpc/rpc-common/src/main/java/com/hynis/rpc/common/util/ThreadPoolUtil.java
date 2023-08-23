package com.hynis.rpc.common.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author hynis
 * @date 2023/8/3 21:51
 * 线程池类
 */
public class ThreadPoolUtil {
    /**
     * 创建线程池：
     * 七个参数依次是：核心线程数量、最大线程数量、保活时间、时间单位、任务队列、线程池工厂、拒绝策略
     * 线程池的工作流程：收到任务后，如果线程数小于等于corePoolSize，那么立即创建线程。
     * 如果大于corePoolSize，会进入任务队列排队
     * 如果任务队列排满，那么会额外创建新的线程，直到等于maxPoolSize
     * 对于额外创建的队列，如果处于空闲状态并在保活时间结束，会被销毁
     * @param name
     * @param corePoolSize
     * @param maxPoolSize
     * @return
     */
    public static ThreadPoolExecutor createThreadPool(final String name, int corePoolSize, int maxPoolSize, long keepAliveTime) {
        ThreadPoolExecutor serverHandlerPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue(1000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "hynisRpc-" + name + "-" + r.hashCode());
                    }
                },
                new ThreadPoolExecutor.AbortPolicy());

        return serverHandlerPool;
    }
}
