package com.hynis.rpc.common.zookeeper;

/**
 * @author hynis
 * @date 2023/8/3 10:56
 */
public class ZookeeperConstant {
    public static final int ZK_SESSION_TIMEOUT = 5000;
    public static final int ZK_CONNECTION_TIMEOUT = 5000;

    public static final String ZK_REGISTRY_PATH = "/registry";
    public static final String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";

    public static final String ZK_NAMESPACE = "hynisRpc";
}
