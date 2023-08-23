package com.hynis.rpc.test.client;

import com.hynis.rpc.client.core.RpcClient;
import com.hynis.rpc.client.handler.RpcFuture;
import com.hynis.rpc.client.proxy.RpcService;
import com.hynis.rpc.test.service.HelloService;

import java.util.concurrent.TimeUnit;

/**
 * @author hynis
 * @date 2023/8/21 10:28
 */
public class RpcClientBootStrap {
    public static void main(String[] args) throws InterruptedException {
        final RpcClient rpcClient = new RpcClient("192.168.235.151:2181");

        // threadNum个线程各自发送requestNum个请求
        int threadNum = 1;
        final int requestNum = 50;
        Thread[] threads = new Thread[threadNum];

        long startTime = System.currentTimeMillis();
        //benchmark for sync call
        for (int i = 0; i < threadNum; ++i) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < requestNum; i++) {
                        try {
                            RpcService client = rpcClient.createAsyncService(HelloService.class, "1.0");
                            RpcFuture helloFuture = client.call("hello", Integer.toString(i));
                            String result = (String) helloFuture.get(3000, TimeUnit.MILLISECONDS);
                            if (!result.equals("Hi " + i)) {
                                System.out.println("error = " + result);
                            } else {
                                System.out.println("result = " + result);
                            }
                            try {
                                Thread.sleep(5 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            System.out.println(e.toString());
                        }
                    }
                }
            });
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        long timeCost = (System.currentTimeMillis() - startTime);
        String msg = String.format("Sync call total-time-cost:%sms, req/s=%s", timeCost, ((double) (requestNum * threadNum)) / timeCost * 1000);
        System.out.println(msg);

        rpcClient.stop();
    }
}
