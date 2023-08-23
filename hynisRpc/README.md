# hynisRpc开发文档

## 写在前面

> 1、项目参考

hynisRpc参考了NettyRpc项目

> 2、为什么要叫**hynis**？

因为我的昵称是hynis，音译自单词Highness

> 3、轮子用到的巧招
- 单例模式：静态内部类实现单例
- 动态代理：JDK动态代理、CGlib动态代理
- Netty：实现C/S、理解Netty中Channel、Pipeline、EventLoop的关系
- 如何解决粘包、半包问题
- Rpc协议的设计
- 并发控制：AQS、ConcurrentHashMap、CopyOnWriteArraySet

## 如何运行

clone项目，本服务依赖Zookeeper的运行

1. 启动Zookeeper

```sh
# docker安装Zookeeper服务
docker pull zookeeper
# 运行Zookeeper，映射端口到2181
docker run --name my-zookeeper -p 2181:2181 -d zookeeper
# 进入执行后的镜像
docker exec -it <container_name_or_id> /bin/bash
# 进入Zookeeper的命令行
zkCli.sh
```

执行命令

```sh
# 查看Zookeeper目录
ls /<path>
# 查看节点内容
get /<path>
```

2. 启动RpcServerBootStrap

配置IP与Port后，执行服务端，服务端将会扫描所有带有`@RpcService`注解的服务，并注册到Zookeeper

```java
@RpcService(value = HelloService.class, version = "1.0")
public class HelloServiceImpl implements HelloService {
    public static final String HELLO_PREFIX = "hello";
    @Override
    public String hello(String name) {
        return HELLO_PREFIX + ":" + name;
    }
}
```

3. 启动RpcClientBootStrap

配置IP与Port后，Client本地调用Zookeeper上的方法，得到运行结果

## 技术选型

- **C/S端**：Netty——Java实现的NIO最优秀的框架
- **服务注册与发现**：Zookeeper
- **序列化与反序列化**：Protostuff、Kyro等
- **动态代理**：JDK动态代理、CG lib动态代理
- **负载均衡**

## hynisRpc结构

### 需要满足的功能

首先我们需要明确，一个RPC框架需要满足那些功能？

作为一个RPC，一切的目的都是**远程过程调用**，即给用户提供一种几乎与运行本地方法无异的服务。

为了这个目的，我们需要做一些辅助的或是增强的功能来更好的完成这个核心目的。

1. **序列化与反序列化**：将对象在内存中的表示转换为一种可以存储或传输的格式，以便稍后可以重新还原为相同的对象。这可以是二进制格式（一个`byte`数组），也可以是文本格式（`json`或是`xml`）
2. **服务注册和发现机制**：使客户端能够动态地发现可用的服务端实例，适应动态变化的服务拓扑
   - 使用Zookeeper可以很好的完成这个功能
3. **负载均衡**：使Client的请求均匀的打到服务器上

### 项目模块划分

hynisRpc项目一共划分为四个模块

- `rpc-server`：服务器端
- `rpc-client`：客户端
- `rpc-common`：通用类
- `rpc-test`：测试模块

## rpc-server模块

> Server需要具备的功能

作为Server，基本的功能有：

1. 服务器的启动与关闭；
2. 可以接收请求，处理请求；
3. 还需要实现**服务注册**功能

因此我建立了如此的继承体系：

- `Server`：提供抽象方法`start`与`stop`
  - `NettyServer`：建立Netty服务器，提供添加服务的`addService`方法
    - `RpcNettyServer`：扫描所有带有`@RpcService`注解的类，存于Map内

### Server

提供基本的启动与关闭方法：

```java
public abstract class Server {
    public abstract void start();
    public abstract void stop();
}
```

### NettyServer

（此节涉及到Netty相关知识，关于Netty[可以参考此篇博客](https://www.yesmylord.cn/2021/10/06/Netty/Netty/?highlight=netty#Netty%E6%A6%82%E8%BF%B0)）

Server端的入站处理器和出站处理器由以下部件构成：

**入站处理器**：负责处理从网络到应用程序的数据，数据进入服务器后，会按顺序依次流过以下处理器handler。

```java
pipeline.addLast(new IdleStateHandler(0, 0, Beat.BEAT_TIMEOUT, TimeUnit.SECONDS));
pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
pipeline.addLast(new RpcMsgDecoder(RpcResponse.class, serializer));
pipeline.addLast(new RpcServerHandler(handlerMap, threadPoolExecutor));
```

- `IdleStateHandler`：【Netty自带】处理服务器空闲状态的处理器，在读/写空闲时间到后，会触发对应的事件
- `LengthFieldBasedFrameDecoder`：【Netty自带】一般情况下，数据帧的格式是由通信协议规定的，`LengthFieldBasedFrameDecoder`需要根据具体协议来配置这些参数，我们的数据帧的格式由**RpcRequest**类实现
- `RpcMsgDecoder`：【自定义实现】解码器，调用反序列化工具，将二进制流或是文本格式转换为内存中的对象
- `RpcServerHandler`：【自定义实现】负责执行客户端所需要的服务，从**request**获取反射需要的信息，之后通过动态代理调用对应的方法进行执行，返回**response**

**出站处理器**：出站处理器负责处理从应用程序到网络的数据

```java
pipeline.addLast(new RpcMsgEncoder(RpcRequest.class, serializer));
```

- `RpcMsgEncoder`：【自定义实现】编码器，负责调用序列化工具

### RpcServer

负责扫描所有带有注解`@RpcService`的服务，实现了Spring的三个接口：

- `ApplicationContextAware`：重写`setApplicationContext`方法，就可以获取Spring上下文，比如使用`ctx.getBeansWithAnnotation(RpcService.class)`获取所有拥有注解RpcService的接口
- `InitializingBean`：重写`afterPropertiesSet`方法，可以用于执行额外的初始化操作
- `DisposableBean`：重写`destroy`方法，额外的销毁方法

```java
@Override
public void setApplicationContext(ApplicationContext ctx) throws BeansException {
    Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RpcService.class);
    if (serviceBeanMap != null && !serviceBeanMap.isEmpty()) {
        for (Object serviceBean : serviceBeanMap.values()) {
            RpcService service = serviceBean.getClass().getAnnotation(RpcService.class);
            String interfaceName = service.value().getName();
            String version = service.version();
            // 此方法将服务信息存于一个hashmap，servicemap内
            super.addService(interfaceName, version, serviceBean);
        }
    }
}
```

### ServiceRegister服务注册

>ZooKeeper 实现 RPC 服务注册与发现的主要流程是：
>
>1. 服务提供者（即Server）将自己的信息注册到 ZooKeeper 上的一个节点，通常是临时节点。
>2. 服务消费者（即Client）监听特定服务的节点变化，获取可用的服务提供者列表。
>3. 服务消费者根据负载均衡策略选择一个服务提供者节点，并进行远程调用。
>4. 在服务提供者下线或注册信息变化时，ZooKeeper 会通知消费者，消费者相应地更新可用节点列表。

服务注册的数据流向步骤：

1、原始数据，使用`@RpcService`注解标记

```java
@RpcService(value = HelloService.class, version = "1.0")
public class HelloServiceImpl implements HelloService {
    public static final String HELLO_PREFIX = "hello";
    @Override
    public String hello(String name) {
        return HELLO_PREFIX + ":" + name;
    }
}
```

2、在RpcServer中被扫描，被添加到serviceMap中

3、Server端启动，主动进行注册，将serviceMap的数据封装到`RpcServiceInfo`内

```java
@Data
public class RpcServiceInfo implements Serializable {
    private String serviceName;
    private String version;
}
```

4、将所有的`RpcServiceInfo`与host、port一起封装到`RpcProtocol`

```java
@Data
public class RpcProtocol {
    private String host;
    private int port;
    private List<RpcServiceInfo> serviceInfoList;
    
    public String toJson() {
        return JsonUtil.objectToJson(this);
    }
    public static RpcProtocol fromJson(String json) {
        return JsonUtil.jsonToObject(json, RpcProtocol.class);
    }
}
```

5、`RpcProtocol`序列化为Json串，通过Curator，创建在Zookeeper的临时节点

6、添加监听器， 一旦对应节点的连接状态重新连接，那么就重新进行注册服务

```java
curatorClient.addConnectionStateListener(new ConnectionStateListener() {
    // 一旦对应节点的连接状态重新连接，那么就重新进行注册服务
    @Override
    public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
        if (connectionState == ConnectionState.RECONNECTED) {
            log.info("Connection state: {}, register service after reconnected", connectionState);
            registerService(host, port, serviceMap);
        }
    }
});
```

### Server端总结

Server端实现了三种功能：

1. 服务器的启动与关闭：使用Netty建立高性能的NIO服务器
2. 接收请求，处理请求：通过Netty通道，使用多个handler处理请求
3. 服务注册：通过扫描注解，json化后发给Zookeeper
4. 序列化：序列化的形式可能是文本形式（json）或是二进制形式（byte数组）
   - json形式：用于给Zookeeper创建临时节点
   - 二进制形式：BS之间通信，缩小传输量，加快传输速度

## rpc-client模块

> Client需要具备的功能

作为Client，基本的功能有：

1. 客户端启动后，可以接收请求，处理请求；
2. 可以创建请求
4. 还需要实现**服务发现**功能

此处以一个方法调用远程方法来介绍Client模块使用的各个部件，之后再详细介绍

现在客户端需要调用服务

1. 启动客户端RpcClient
2. RpcClient创建后，会开始服务发现，创建ServiceDiscovery并连接Zookeeper，将服务信息更新到`rpcProtocolSet`、`connectedServerNodeMap`
3. RpcClient使用代理ObjectProxy创建异步请求
4. ConnectionManager会给此请求分配一个处理器RpcHandler，将接口信息、版本号封装为RpcRequest
5. 处理器发送RpcRequest，返回RpcFuture
6. 从RpcFuture获得最终执行结果

### RpcClient&ConnectManager

RpcClient并不真正的进行Netty连接，来执行真正的操作的是ConnectManager，

ConnectManager是用静态内部类实现的单例类，保证全局唯一。

ConnectManager维护了两个重要的集合及监听器，来保证服务端的服务都会更新到本地。

```java
// rpcProtocolSet是一个集合，存放所有的RpcProtocol，Zookeeper服务器上注册的所有服务都在这里
// CopyOnWriteArraySet是一个集合类，使用数组实现，采用了COW的思想，适合用在读远远大于写的场景
// 而服务端的服务一般不会发生巨大变化，因此适合使用
private CopyOnWriteArraySet<RpcProtocol> rpcProtocolSet = new CopyOnWriteArraySet<>();

// connectedServerNodeMap存放每个服务器与处理器的映射关系
private Map<RpcProtocol, RpcClientHandler> connectedServerNodeMap = new ConcurrentHashMap<>();
```

ConnectManager在连接对应RpcProtocol的服务器时，会发起Netty连接，连接的配置如下，与Server端类似。

```java
// 入站处理器
cp.addLast(new IdleStateHandler(0, 0, Beat.BEAT_INTERVAL, TimeUnit.SECONDS));
cp.addLast(new RpcMsgEncoder(RpcRequest.class, serializer));
cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
cp.addLast(new RpcClientHandler());
// 出站处理器
cp.addLast(new RpcMsgDecoder(RpcResponse.class, serializer));
```

只有RpcClientHandler需要我们特别关心，RpcClientHandler负责将我们的请求封装为RpcFuture，等待异步获取结果。



### ServiceDiscovery

服务发现类在创建时就连接Zookeeper服务器。

获取所有Zookeeper的节点，遍历所有节点，获取对应Zookeeper服务器上的所有服务（将以二进制形式返回），然后将其反序列化为`RpcProtocol`列表，然后发送给ConnectManager。

RpcProtocol的结构如下：他表示一个服务器（`host:port`）上的所有服务

```java
public class RpcProtocol {
    private String host;
    private int port;
    private List<RpcServiceInfo> serviceInfoList;

    public String toJson() {
        return JsonUtil.objectToJson(this);
    }
    public static RpcProtocol fromJson(String json) {
        return JsonUtil.jsonToObject(json, RpcProtocol.class);
    }
}
```











# API解释及并发技巧

## IdleStateHandler

- IdleStateHandler的构造函数有三个参数：

1. `readerIdleTimeSeconds`: 读空闲时间，即连接在多长时间内没有接收到数据将触发`userEventTriggered`方法中的`ReaderIdleStateEvent`事件。
2. `writerIdleTimeSeconds`: 写空闲时间，即连接在多长时间内没有发送数据将触发`userEventTriggered`方法中的`WriterIdleStateEvent`事件。
3. `allIdleTimeSeconds`: 读写空闲时间，即连接在多长时间内没有进行读写操作将触发`userEventTriggered`方法中的`AllIdleStateEvent`事件。

使用方式如下：

```java
public class MyIdleHandler extends ChannelInboundHandlerAdapter {
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                // 处理读空闲事件
            } else if (event.state() == IdleState.WRITER_IDLE) {
                // 处理写空闲事件
            } else if (event.state() == IdleState.ALL_IDLE) {
                // 处理读写空闲事件
            }
        }
    }
}
```

## LengthFieldBasedFrameDecoder

- LengthFieldBasedFrameDecoder

使用`LengthFieldBasedFrameDecoder`时，需要提供几个参数：

1. `maxFrameLength`: 数据帧的最大长度，超过这个长度的帧将被丢弃。
2. `lengthFieldOffset`: 长度字段的偏移量，表示长度字段在帧中的位置。
3. `lengthFieldLength`: 长度字段的长度，表示长度字段本身占用的字节数。
4. `lengthAdjustment`: 长度字段的调节值，表示帧长度字段的值与帧的实际长度之间的差值。
5. `initialBytesToStrip`: 跳过的字节数，表示从解码帧中去掉的字节数。

一般情况下，数据帧的格式是由通信协议规定的，`LengthFieldBasedFrameDecoder`需要根据具体协议来配置这些参数。

## CopyOnWrite

[一篇关于COW很好的文章](https://coolshell.cn/articles/11175.html)

> COW思想：即写时复制
>
> 从JDK1.5开始Java并发包里提供了两个使用CopyOnWrite机制实现的并发容器,它们是`CopyOnWriteArrayList`和`CopyOnWriteArraySet`

**原理**：当有修改操作时，将原内容复制出一个副本，在副本上进行修改。修改完成后，将指针指向副本，从而达到无需使用“锁”的目的。

**本质**：

- COW的思想本质是一种“乐观锁”，即总是认为没有发生并发写操作
- COW也是一种读写分离的思想，读与写使用了两块内存

**弊端**：

- 较为占用内存，最好不要使用COW存储那些大对象；
- 实现了最终一致性，而不是实时一致性。





## Netty中Channel、EventLoop、Pipeline的关系

每个 `Channel` 都关联一个 `EventLoop`；而每个 `Channel` 又有一个 `Pipeline`；

1. **Channel（通道）：** `Channel` 是网络通信的抽象，它代表了一个底层的通信连接，可以是网络套接字、文件、管道等。
   - `Channel` 提供了一种读取和写入数据的抽象接口，用于处理数据的传输。
   - 每个 `Channel` 都与一个 `EventLoop` 关联，用于处理事件和任务。
2. **EventLoop（事件循环）：** `EventLoop` 是 Netty 处理事件和任务的核心机制。
   - 本质是一个Thread+Selector模型，它会不断地循环，等待事件的发生，然后处理这些事件。
   - 每个 `EventLoop` 都在一个单独的线程中运行。
   - 每个 `Channel` 都会被注册到一个特定的 `EventLoop` 上，这样 `EventLoop` 负责处理该通道的所有 I/O 操作和事件。
3. **Pipeline（管道）：** `Pipeline` 是一系列的处理器链，用于处理入站和出站的数据。
   - 每个 `Channel` 都有一个关联的 `Pipeline`，通过这个 `Pipeline` 可以将数据从一个处理器传递到另一个处理器，实现数据的处理、转换和传输。
   - `Pipeline` 是 Netty 实现高度可定制性的关键部分，它允许你按需添加、移除或替换处理器，以构建复杂的数据处理流程。

## SimpleChannelInboundHandler

`SimpleChannelInboundHandler` 是 Netty 框架中用于处理入站消息的处理器。它是一个抽象类，提供了默认的方法实现，供子类继承并实现自定义的逻辑。以下是 `SimpleChannelInboundHandler` 中几个重要方法的执行顺序：

1. **`channelRegistered` 方法：** 当通道（Channel）被注册到 EventLoop 中时调用。这个方法在通道注册之后立即被调用，用于执行一些初始化操作。在这个方法中，你可以访问通道的属性、获取 EventLoop 等。
2. **`channelActive` 方法：** 当通道变为活跃状态时调用。通常发生在通道连接成功后。在这个方法中，你可以执行连接成功后的逻辑，比如发送欢迎消息、准备数据传输等。
3. **`channelRead0` 方法：** 当有入站消息可读取时调用。这是 `SimpleChannelInboundHandler` 最核心的方法。在这个方法中，你可以处理读取到的消息，执行业务逻辑，并可能产生响应消息。
4. **`channelReadComplete` 方法：** 在一批消息读取完成后调用。可以用来执行一些批量处理的逻辑，例如刷新缓冲区或执行其他收尾操作。
5. **`channelInactive` 方法：** 当通道变为非活跃状态时调用。通常发生在通道连接断开后。在这个方法中，你可以执行资源清理、断开后的逻辑等。
6. **`exceptionCaught` 方法：** 当处理过程中出现异常时调用。在这个方法中，你可以捕获和处理异常，避免因异常而导致应用程序崩溃。

需要注意的是，Netty 的事件处理是异步的，不同的事件可能会在不同的线程上执行。因此，编写代码时需要考虑线程安全和同步问题。另外，每个方法的执行顺序还可能受到整个处理链的配置和调用顺序的影响。在实际使用中，可以根据具体情况在这些方法中编写适当的逻辑。

## 动态代理

## AQS抽象同步队列

抽象同步队列**AQS**：[查看AQS](https://www.yesmylord.cn/2021/08/10/JUC/Java%E7%BA%BF%E7%A8%8B%E4%B8%8E%E5%B9%B6%E5%8F%91/#AQS%E5%90%8C%E6%AD%A5%E6%8A%BD%E8%B1%A1%E9%98%9F%E5%88%97)

双端队列，可以简便的管理线程同步操作

一般都是作为静态内部类来实现

```java
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
```





