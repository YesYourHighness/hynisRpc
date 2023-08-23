package com.hynis.rpc.client.handler;

import com.hynis.rpc.client.core.RpcClient;
import com.hynis.rpc.common.entity.RpcRequest;
import com.hynis.rpc.common.entity.RpcResponse;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author hynis
 * @date 2023/8/17 20:23
 *
 * Rpc异步调用工具：判断Rpc操作是否完成，若完成执行相关回调函数
 */
@Slf4j
public class RpcFuture implements Future<Object> {

    /**
     * 静态内部类，AQS同步控制器
     */
    private Sync sync;
    /**
     * 请求，构造时传入
     */
    private RpcRequest request;
    /**
     * 响应，请求成功后设置此值
     */
    private RpcResponse response;
    /**
     * 标记开始时间
     */
    private long startTime;
    /**
     * 响应阈值，5s
     */
    private long responseTimeThreshold = 5000;
    /**
     * 回调函数等待队列
     */
    private List<AsyncRpcCallback> pendingCallbacks = new ArrayList<>();
    /**
     * 可重入锁，搭配pendingCallbacks使用
     */
    private ReentrantLock lock = new ReentrantLock();



    public RpcFuture(RpcRequest request) {
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }



    /**
     * 不支持撤销与判断是否撤销的操作
     * @return
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    /**
     * 不支持撤销与判断是否撤销的操作
     * @return
     */
    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    /**
     * 在异步操作完成时被调用，以设置响应结果并触发相应的回调
     * 1、将请求的响应值赋值
     * 2、释放锁，通知其他线程执行
     * 3、唤醒回调方法
     * 4、计算异步操作的响应时间，并在响应时间超过阈值时记录警告日志。这段代码用于监控异步操作的性能。
     * @param response
     */
    public void done(RpcResponse response) {
        this.response = response;
        sync.release(1);
        invokeCallbacks();
        // 计算异步操作的响应时间，并在响应时间超过阈值时记录警告日志。这段代码用于监控异步操作的性能。
        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            log.warn("Service response time is too slow. Request id = " + response.getRequestId() + ". Response Time = " + responseTime + "ms");
        }
    }

    /**
     * 运行回调函数
     */
    private void invokeCallbacks() {
        lock.lock();
        try {
            for (final AsyncRpcCallback callback : pendingCallbacks) {
                runCallback(callback);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 执行回调函数
     * @param callback
     */
    private void runCallback(final AsyncRpcCallback callback) {
        final RpcResponse res = this.response;
        RpcClient.submit(new Runnable() {
            @Override
            public void run() {
                if (!res.isError()) {
                    callback.success(res.getResult());
                } else {
                    callback.fail(new RuntimeException("Response error", new Throwable(res.getError())));
                }
            }
        });
    }

    /**
     * 添加回调函数：
     * 如果操作处于空闲，那么就执行回调
     * 否则就加入队列
     * @param callback
     * @return
     */
    public RpcFuture addCallback(AsyncRpcCallback callback) {
        lock.lock();
        try {
            if (isDone()) {
                runCallback(callback);
            } else {
                this.pendingCallbacks.add(callback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    /**
     * 如果异步操作已经完成，get() 方法会立即返回结果。
     * 如果异步操作尚未完成，get() 方法会阻塞当前线程，直到异步操作完成并返回结果
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Override
    public Object get() throws InterruptedException, ExecutionException {
        // acquire 获取一个同步资源，数量为 1。
        // 如果当前资源可用（状态为允许获取），线程将立即获得该资源并继续执行。
        // 如果资源不可用（状态为等待），线程将被阻塞，直到资源可用或者等待超时。
        sync.acquire(1);
        if (this.response != null) {
            return this.response.getResult();
        } else {
            return null;
        }
    }

    /**
     * 带有超时时间的get
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(1, unit.toNanos(timeout));
        if (success) {
            if (this.response != null) {
                return this.response.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
                    + ". Request class name: " + this.request.getClassName()
                    + ". Request method: " + this.request.getMethodName());
        }
    }

    /**
     * 抽象同步队列AQS：
     * 双端队列，可以简便的管理线程同步操作
     * 一般都是作为静态内部类来实现
     */
    static class Sync extends AbstractQueuedSynchronizer {
        /**
         * 表示操作已经完成，1表示完成
         */
        private final int done = 1;
        /**
         * 表示操作还未完成，0表示未完成
         */
        private final int pending = 0;

        /**
         * 尝试获得锁：
         * 在AQS的实现中有一个共享资源volatile修饰的state状态，
         * AQS使用state状态表示当前操作的状态
         * getState返回state的值，
         * @param arg arg参数比较特殊，此处并没有使用到这个方法，
         *            但在其他情况下，arg 参数可能会被用来表示请求的资源数量，
         *            或者用来控制获取锁的行为。
         *            例如，在一个可重入的锁实现中，
         *            arg 参数可以用来记录线程获取锁的次数，以便正确处理锁的释放。
         * @return 如果是1，表示操作已经完成，返回true，将释放锁，反之将会失败
         *         注意：返回false并不会阻塞尝试获取锁的线程
         */
        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        /**
         * 尝试释放锁，compareAndSetState表示CAS操作，它会尝试比较两个值
         * compareAndSetState(pending, done)
         * 尝试将state从pending 0设置为done 1，如果成功设置，则锁已经释放
         * 如果设置失败，那么说明当前state仍被占用
         * @param arg
         * @return
         */
        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        protected boolean isDone() {
            return getState() == done;
        }
    }
}
