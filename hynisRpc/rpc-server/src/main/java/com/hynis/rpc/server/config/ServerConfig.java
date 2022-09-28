package com.hynis.rpc.server.config;

import org.springframework.context.annotation.Configuration;

/**
 * @author hynis
 * @date 2022/2/24 0:22
 * 服务端配置类
 */
@Configuration
public class ServerConfig {
    public static final int SERVER_PORT = 8080;
    public static final int SO_BACKLOG_SIZE = 1024;
}
