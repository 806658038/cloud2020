# SpringCloud



可借鉴 博客笔记 `https://blog.csdn.net/qq_36903261/article/details/106507150`



### 一、建立Maven父项目

1.更改POM文件，引入依赖

用 父工程    <dependencyManagement>  </dependencyManagement> 来管理子模块的依赖。



**2.建立服务提供者模块 cloud-provider-payment8001**

**3.引入依赖，配置yml文件**

```yml
server:
  port: 8001

spring:
  application:
    name: cloud-payment-service

  datasource:
    type: com.alibaba.druid.pool.DruidDataSource            # 当前数据源操作类型
    driver-class-name: com.mysql.jdbc.Driver             # mysql驱动包
    url: jdbc:mysql://localhost:3306/dbcloud?useUnicode=true&characterEncoding=utf-8&useSSL=false
    username: root
    password: 123456

mybatis:
  type-aliases-package: com.hzh.springcloud.pojo     # 别名 映射
  configuration:
    map-underscore-to-camel-case: true  # 驼峰命名法
  mapper-locations: classpath:mapper/*.xml
```

和SpringBoot一样的步骤写pojo，dao，service，controller 层。写好增删改查方法。

controller 层 用Resultful风格 并且  方法返回的全是 JSNO对象，

```
Get  获取资源
Put  更新资源
Delete 删除资源
Post 创建资源
```

```java
@RestController  // 全部返回JSON
@Slf4j	// 可以写日志
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // 一定要加@RequestBody，否则 consumer 操作时，插入的数据会为null
    @PostMapping(value = "/payment/create")
    public CommonResult create(@RequestBody Payment payment){
        int result = paymentService.create(payment);
        log.info("***插入结果:"+result);
        if(result>0){
            return new CommonResult(200,"插入数据库成功",result);
        }else{
            return new CommonResult(444,"插入数据库失败",null);
        }
    }

    @GetMapping(value = "/payment/get/{id}")
    public CommonResult getPaymentById(@PathVariable("id") Long id){
        Payment payment = paymentService.getPaymentById(id);
        log.info("***获取结果:"+payment);

        if(payment!=null){
            return new CommonResult(200,"查询成功",payment);
        }else{
            return new CommonResult(444,"没有对应的记录,查询ID"+id,null);
        }
    }


}
```

启动测试，成功



### 二、新建消费者模块

cloud-consumer-order81

1.引入依赖，yml配置端口即可

写 pojo，controller，config



**2.配置RestTemplate**

RestTemplate提供了多种便捷访问远程Http服务的方法，

是一种简单便捷的访问restful服务的模板类，是spring提供的用于访问Rest服务的客户端模板工具集。



自定义配置类

注册 RestTemplate 来

Controller 层

```java
@RestController
public class OrderController {
	// 这样写死来，不能实现轮询
    public static final String PAYMENT_URL="http://localhost:8001";

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/consumer/payment/create")
    public CommonResult<Payment> creat(Payment payment){
        return restTemplate.postForObject(PAYMENT_URL+"/payment/create",payment,CommonResult.class);

    }

    @GetMapping("/consumer/payment/get/{id}")
    public CommonResult<Payment> getPayment(@PathVariable("id") Long id){

        return restTemplate.getForObject(PAYMENT_URL+"/payment/get/"+id,CommonResult.class);

    }

}
```

```
服务提供者
http://localhost:8001/payment/get/2
消费方
http://localhost:81/consumer/payment/get/2

二者是一样的，则成功
```



### 三、工程重构

这样的微服务 比较冗余，开始重构改进

1.新建  cloud-api-commons 模块

它来管理 pojo，其余项目的pojo删除，其余项目引入 cloud-api-commons



### 四、Eureka



**1.什么是服务治理**

​	Spring Cloud封装了 Netflix公司开发的 Eureka模块来实现服务治理

​	在传统的rpc远程调用框架中,管理每个服务与服务之间依赖关系比较复杂,管理比较复杂,所以需要使用服务治理，管理服务于服务之间依赖关系,可以实现服务调用、负载均衡、容错等,实现服务发现与注册。



1、Eureka是什么

**Eureka** 是 **Netflix**开发的，一个基于 REST 服务的，服务注册与发现的组件，以实现中间层服务器的负载平衡和故障转移。

它主要包括两个组件：Eureka Server 和 Eureka Client

- Eureka Client：一个Java客户端，用于简化与 Eureka Server 的交互（通常就是微服务中的客户端和服务端）
- Eureka Server：提供服务注册和发现的能力（通常就是微服务中的注册中心）



服务在Eureka上注册，然后每隔30秒发送心跳来更新它们的租约。如果客户端不能多次续订租约，那么它将在大约90秒内从服务器注册表中剔除。注册信息和更新被复制到集群中的所有eureka节点。来自任何区域的客户端都可以查找注册表信息（每30秒发生一次）来定位它们的服务（可能在任何区域）并进行远程调用



2、 Eureka 客户端与服务器之间的通信

服务发现有两种模式：一种是客户端发现模式，一种是服务端发现模式。Eureka采用的是客户端发现模式。



2.1. Register（注册）

Eureka客户端将关于运行实例的信息注册到Eureka服务器。注册发生在第一次心跳。



2.2. Renew（更新 / 续借）

Eureka客户端需要更新最新注册信息（续借），通过每30秒发送一次心跳。更新通知是为了告诉Eureka服务器实例仍然存活。如果服务器在90秒内没有看到更新，它会将实例从注册表中删除。建议不要更改更新间隔，因为服务器使用该信息来确定客户机与服务器之间的通信是否存在广泛传播的问题。



2.3. Fetch Registry（抓取注册信息）

Eureka客户端从服务器获取注册表信息并在本地缓存。之后，客户端使用这些信息来查找其他服务。通过在上一个获取周期和当前获取周期之间获取增量更新，这些信息会定期更新(每30秒更新一次)。获取的时候可能返回相同的实例。Eureka客户端自动处理重复信息。



2.4. Cancel（取消）

Eureka客户端在关机时向Eureka服务器发送一个取消请求。这将从服务器的实例注册表中删除实例，从而有效地将实例从流量中取出。



### 4、Eureka VS Zookeeper

4.1. Eureka保证AP

Eureka服务器节点之间是对等的，只要有一个节点在，就可以正常提供服务。

Eureka客户端的所有操作可能需要一段时间才能在Eureka服务器中反映出来，随后在其他Eureka客户端中反映出来。也就是说，客户端获取到的注册信息可能不是最新的，它并不保证强一致性

4.2. Zookeeper保证CP

Zookeeper集群中有一个Leader，多个Follower。Leader负责写，Follower负责读，ZK客户端连接到任何一个节点都是一样的，写操作完成以后要同步给所有Follower以后才会返回。如果Leader挂了，那么重新选出新的Leader，在此期间服务不可用。

4.3. 为什么用Eureka

分布式系统大都可以归结为两个问题：数据一致性和防止单点故障。而作为注册中心的话，即使在一段时间内不一致，也不会有太大影响，所以在A和C之间选择A是比较适合该场景的。



### 工程实现步骤

1、新建cloud-eureka-server7001模块

pom.xml中加入依赖：

```
  <!--eureka-server-->
        <!--eureka-server-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```



2、resources目录下新建application.yml

```yml
server:
  port: 7001

eureka:
  instance:
    hostname: localhost    #eureka服务端的实例名称
  client:
    register-with-eureka: false    #false表示不向注册中心注册自己。
    fetch-registry: false     #false表示自己端就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    service-url:
    #集群指向其它eureka
      #defaultZone: http://eureka7002.com:7002/eureka/
    #单机就是 自己
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```

3、编写启动类EurekaApplication7001

```java
@EnableEurekaServer	 //开启Eureka 注册 
@SpringBootApplication
public class EurekaApplicatin7001 {

    public static void main(String[] args) {
        SpringApplication.run(EurekaApplicatin7001.class, args);
    }

}
```

服务提供方引入依赖

```
<!--eureka-client-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```



yml 加入下面的配置

```yml
eureka:
  client:
    #表示是否将自己注册进EurekaServer默认为true。
    register-with-eureka: true
    #是否从EurekaServer抓取已有的注册信息，默认为true。单节点无所谓，集群必须设置为true才能配合ribbon使用负载均衡
    fetchRegistry: true
    service-url:
      #单机版
      defaultZone: http://localhost:7001/eureka

```



启动类加上 @EnableEurekaClient

```java
@EnableEurekaClient
@SpringBootApplication
public class PaymentMain8001 {

    public static void main(String[] args) {
        SpringApplication.run(PaymentMain8001.class,args);

    }

}
```

测试，顺利把服务注册到 Eureka服务中心



### 五、Eureka集群

**原理说明：**

服务注册：将服务信息注册到注册中心

服务发现：从注册中心获取服务信息

实质：存key服务名，取value调用地址



**问题：**微服务RPC远程调用最核心的是说明？

高可用，如果注册中心只有一个，出现故障就麻烦了。会导致整个服务环境不可用。

**解决办法**：搭建eureka注册中心集群，实现**负载均衡+故障容错**

互相注册，相互守望

集群搭建步骤

2、修改C:\Windows\System32\drivers\etc下的hosts

```
127.0.0.1 eureka7001.com
127.0.0.1 eureka7002.com
```



修改7001项目 applicaton.yml

```yaml
server:
  port: 7001

eureka:
  instance:
    hostname: localhost    #eureka服务端的实例名称
  client:
    register-with-eureka: false    #false表示不向注册中心注册自己。
    fetch-registry: false     #false表示自己端就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    service-url:
    #集群指向其它eureka
      defaultZone: http://eureka7002.com:7002/eureka/
      
```

修改7002项目 applicaton.yml

```
server:
  port: 7002

eureka:
  instance:
    hostname: localhost    #eureka服务端的实例名称
  client:
    register-with-eureka: false    #false表示不向注册中心注册自己。
    fetch-registry: false     #false表示自己端就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    service-url:
    #集群指向其它eureka
      defaultZone: http://eureka7001.com:7001/eureka/
```

启动验证，输入 http://eureka7001.com:7001/    和 http://eureka7002.com:7002/

测试成功



6、支付和订单两个微服务注册到eureka集群

更改 defaultZone 就行

```html
 service-url:
	# 集群版
      defaultZone: http://eureka7001.com:7001/eureka/, http://eureka7001.com:7001
```



测试

想启动两个 Eureka，再启动服务提供者，再启动消费者成功





### 六、搭建支付服务集群

参照8001服务 搭建 8002服务

启动，访问的总是8001，改配置

在消费者的Controller

```java
public class OrderController {
	.......
public static final String PAYMENT_URL="http://localhost:8001";

}
```



改为服务名

```java
// 写服务名更好
public static final String PAYMENT_URL="http://CLOUD-PAYMENT-SERVICE";
```

同时加上负载均衡

```java
@Bean
@LoadBalanced
public RestTemplate getRestTemplate(){
    return new RestTemplate();
}
```

测试，成功。8001和8002的接口轮流出现



若想修改服务名和访问地址，可以在yml 加入配置

```yml
instance:
  instance-id: payment8001   # 服务名称修改为 payment8001  不再是 localhost:cloud-payment-service:8001
  prefer-ip-address: true     # 访问路径可以显示 ip地址
```





### 七、Eureka自我保护模式

如果 Eureka 服务器检测到超过预期数量的注册客户端以一种不优雅的方式终止了连接，并且同时正在等待被驱逐，那么它们将进入自我保护模式。这样做是为了确保灾难性网络事件不会擦除eureka注册表数据，并将其向下传播到所有客户端。

如果 Eureka 服务器检测到超过预期数量的注册客户端以一种不优雅的方式终止了连接，并且同时正在等待被驱逐，那么它们将进入自我保护模式。这样做是为了确保灾难性网络事件不会擦除eureka注册表数据，并将其向下传播到所有客户端。



为什么会产生自我保护机制？

为防止EurekaClient可以正常运行，但是与EurekaServer网络不同的情况下，EurekaServer不会立刻将EurekaClient服务剔除。



什么是自我保护机制？

默认情况下，当Eureka server在一定时间内没有收到实例的心跳，便会把该实例从注册表中删除（默认是90秒），但是，如果短时间内丢失大量的实例心跳，便会触发eureka server的自我保护机制，会出现红色的警告。

从警告中可以得知，eureka认为虽然收不到实例的心跳，但它认为实例还是健康的，eureka会保护这些实例，不会把它们从注册表中删掉。

在自我保护模式中，EurekaServer会保护服务注册表中的信息，不再注销任何服务实例。



如何禁止自我保护机制

服务提供者：

```yml
instance:
  instance-id: payment8001  
  prefer-ip-address: true

  # Eureka 客户端（8001）向服务器（7001） 发送心跳的时间间隔，单位为秒（默认是30秒），但现在设为1秒
  lease-renewal-interval-in-seconds: 1
  # Eureka 服务端在收到最后一次心跳后等待时间上限，单位为秒（默认是90秒），超时将剔除服务，但现在设为2秒
  lease-expiration-duration-in-seconds: 2
```

注册中心配置：

```yml
server:
  enable-self-preservation: false # 关闭自我保护机制 保证不可用服务及时清除
  # 2秒内，心跳还没来，就剔除服务
  eviction-interval-timer-in-ms: 2000
```



SpringCloud 整合  zookeeper

docker 安装zookeeper

服务提供者

1.新建 cloud-provider-payment8004

2..pom加入依赖

3.配置yml文件

```yml
server:
  port: 8004

#服务别名----注册zookeeper到注册中心名称
spring:
  application:
    name: cloud-provider-payment
  cloud:
    zookeeper:
      connect-string: 192.168.63.130:2181
```

和 Eureka 一样正常编写 controller

启动类加上 @EnableDiscoveryClient

测试，成功



消费者

1.新建 cloud-consumerconsul-order81

2..pom加入依赖

```yml
server:
  port: 81

spring:
  application:
    name: cloud-consumer-service
  cloud:
    #注册到zookeeper地址
    zookeeper:
      connect-string: 192.168.63.130:2181
```

和 Eureka 一样正常编写 controller

启动类加上 @EnableDiscoveryClient

测试 成功



**问题:**zookeeper中的节点是持久还是临时的？

**答**：临时的。





### 九、consul

Consul是一个服务网格（微服务间的  TCP/IP，负责服务之间的网络调用、限流、熔断和监控）解决方案，它是一个一个分布式的，高度可用的系统，而且开发使用都很简便。它提供了一个功能齐全的控制平面，主要特点是：服务发现、健康检查、键值存储、安全服务通信、多数据中心。





docker 安装Consul

和上述步骤差不多

配置服务提供者

新建项目，导入依赖

配置yml

```yml
###consul服务端口号
server:
  port: 8006

spring:
  application:
    name: consul-provider-payment

####consul注册中心地址
  cloud:
    consul:
      host: 192.168.63.130  # linux 的ip地址  因为是在linux启动的 consul
      port: 8500
      discovery:
        hostname: 172.16.0.201  # 本机的ip地址
        service-name: ${spring.application.name}
```

和 Eureka 一样正常编写 controller

启动类加上 @EnableDiscoveryClient



配置消费者者

yml

```yml
###consul服务端口号
server:
  port: 81

spring:
  application:
    name: cloud-consumer-order
####consul注册中心地址
  cloud:
    consul:
      host: 192.168.63.130
      port: 8500
      discovery:
        hostname: 172.16.0.201  # 本机的ip地址
        service-name: ${spring.application.name}
```

步骤都一样

启动类 都要加上 @EnableDiscoveryClient



### 十、Eureka、Zookeeper、Consul三个注册中心的异同点

| 组件名    | 语言 | 健康检查 | 对外暴露接口 | CAP  | Spring Cloud 集成 |
| --------- | ---- | -------- | ------------ | ---- | ----------------- |
| Eureka    | Java | 可配支持 | HTTP         | AP   | 集成              |
| Consul    | Go   | 支持     | HTTP/DFS     | CP   | 集成              |
| Zookeeper | java | 支持     | 客户端       | CP   | 集成              |



### 十一、Ribbon



Spring Cloud Ribbon是一个基于HTTP和TCP的客户端**负载均衡工具**，它基于Netflix Ribbon实现。



**负载规则替换，注意，不能与主启动类在同一个包下！**

比如启动类在 com.hzh.springcloud 包下

则新建包 com.hzh.myrule

```java
package com.hzh.myrule;

import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRule {

    @Bean
    public IRule myRule(){
        // 自定义为随机
        return new RandomRule();
    }

}
```



主启动类添加注释：

```java
// 必须是大写
@RibbonClient(name ="CLOUD-PAYMENT-SERVICE",configuration = MyRule.class)
@EnableEurekaClient
@SpringBootApplication
public class OrderMain81 {

}
```

测试成功





手写Ribbon之轮询算法

消费方配置

在 com.hzh.springcloud 包下 建立LoadBalancerDemo接口（这样启动类能够扫描得到）

```java
public interface LoadBalancerDemo {

    ServiceInstance instances(List<ServiceInstance> serviceInstances);

}
```

实现接口

```java
@Component
public class MyLoadBalancer implements LoadBalancerDemo {

    private AtomicInteger atomicInteger = new AtomicInteger(0);

    final int getAndIncrement(){
        int current;
        int next;
        do {
            current = this.atomicInteger.get();
            next =current >= Integer.MAX_VALUE? 0:current+1;
        }while (!this.atomicInteger.compareAndSet(current,next));
        System.out.println("***next第:"+next+"次访问");
        return next;
    }

    //负载均衡算法：rest接口第几次请求数 % 服务器集群总数量 = 实际调用服务器位置下标  ，每次服务重启动后rest接口计数从1开始。
    @Override
    public ServiceInstance instances(List<ServiceInstance> serviceInstances) {
       int index=getAndIncrement() % serviceInstances.size();

        return serviceInstances.get(index);
    }

}
```



在配置类中 把 @LoadBalanced 注解 删除（除去原先配置的负载均衡）

启动类 不需要加多余的注解

```java
@EnableEurekaClient
@SpringBootApplication
public class OrderMain81 {

    public static void main(String[] args) {
        SpringApplication.run(OrderMain81.class,args);

    }

}
```

编写 controller层 

```java
    // 将 自己写的注入进来
	@Resource
    private LoadBalancerDemo loadBalancer;
    @Resource
    private DiscoveryClient discoveryClient;
    
    
    @GetMapping(value = "/consumer/payment/lb")
    public String getPaymentLB() {
    List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
        if(instances == null || instances.size() <= 0) {
            return null;
        }
        ServiceInstance serviceInstance = loadBalancer.instances(instances);
        URI uri = serviceInstance.getUri();

        return restTemplate.getForObject(uri+"/payment/lb",String.class);
    }
    
```



8001和8002 服务提供者的controller 写入

```
@GetMapping(value = "/payment/lb")
public String getPaymentLB() {
    return serverPort;
}
```

测试轮询，成功



### 十二、OpenFeign

OpenFeign是什么？

Feign是一个声明式的Web Service客户端。它的出现使开发Web  Service客户端变得很简单。使用Feign只需要创建一个接口加上对应的注解，比如：FeignClient注解。Feign有可插拔的注解，包括Feign注解和JAX-RS注解。Feign也支持编码器和解码器，Spring Cloud Open Feign对Feign进行增强支持Spring MVC注解，可以像Spring  Web一样使用HttpMessageConverters等。



新建cloud-consumer-feign-order81

导入依赖

```
    <dependencies>
        <!--openfeign-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        
        <!--eureka client-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        
        <!-- 引入自己定义的api通用包，可以使用Payment支付Entity -->
        <dependency>
            <groupId>com.hzh.springcloud</groupId>
            <artifactId>cloud-api-commons</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <!--web-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!--一般基础通用配置-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

```

配置yml

```yml
server:
  port: 81

eureka:
  client:
    register-with-eureka: false
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/,http://eureka7002.com:7002/eureka/
```



新建Service 加上 @FeignClient 注解

```java
@Component
@FeignClient("CLOUD-PAYMENT-SERVICE")   // 微服务名称
public interface PaymentFeignService {

    // 这里 要和 服务提供者的一样
    @GetMapping(value = "/payment/get/{id}")  
    public CommonResult getPaymentById(@PathVariable("id") Long id);


}
```

controller

```java
@RestController
@Slf4j
public class OrderController {
    @Autowired
    private PaymentFeignService paymentFeignService;

    @GetMapping(value = "/consumer/payment/get/{id}")
    public CommonResult<Payment> getPaymentById(@PathVariable("id") Long id){

        return paymentFeignService.getPaymentById(id);
    }
    
}
```

启动类 加上 @EnableFeignClients 注解

```java
@EnableFeignClients
@SpringBootApplication
public class OrderMain81 {

    public static void main(String[] args) {
        SpringApplication.run(OrderMain81.class,args);
    }

}
```

测试成功，若是该服务提供者配置了集群，要每个集群的controller都要写，不然会报404错误，因为是轮询。



Feign自带负载均衡



openFeign 客户端 默认等待1秒钟，超时就会报错

配置 yml

```yml
server:
  port: 81

eureka:
  client:
    register-with-eureka: false
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/,http://eureka7002.com:7002/eureka/

# 设置feign客户端超时时间(OpenFeign默认支持ribbon)
ribbon:
  # 指的是建立连接后从服务器读取到可用资源所用的时间
  ReadTimeout: 5000
  # 指的是建立连接所用的时间，适用于网络状况正常的情况下,两端连接所用的时间
  ConnectTimeout: 5000
```



openFeign 日志增强

自定义配置类

```
@Configuration
public class FeignConfig
{
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    
}
```

yml配置  

```
server:
  port: 81

eureka:
  client:
    register-with-eureka: false
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/,http://eureka7002.com:7002/eureka/

# 设置feign客户端超时时间(OpenFeign默认支持ribbon)
ribbon:
  ReadTimeout: 5000
  ConnectTimeout: 5000


logging:
  level:
    # feign日志以什么级别监控哪个接口
    com.hzh.springcloud.service.PaymentFeignService: debug
```



### 十三、Hystrix



**Hystrix介绍**

在微服务场景中，通常会有很多层的服务调用。如果一个底层服务出现问题，故障会被向上传播给用户。我们需要一种机制，当底层服务不可用时，可以阻断故障的传播。这就是断路器的作用。他是系统服务稳定性的最后一重保障。

在springcloud中断路器组件就是Hystrix。Hystrix也是Netflix套件的一部分。他的功能是，当对某个服务的调用在一定的时间内（默认10s），有超过一定次数（默认20次）并且失败率超过一定值（默认50%），该服务的断路器会打开。返回一个由开发者设定的fallback。

fallback可以是另一个由Hystrix保护的服务调用，也可以是固定的值。fallback也可以设计成链式调用，先执行某些逻辑，再返回fallback。



**Hystrix的作用**

1. 对通过第三方客户端库访问的依赖项（通常是通过网络）的延迟和故障进行保护和控制。
2. 在复杂的分布式系统中阻止级联故障。
3. 快速失败，快速恢复。
4. 回退，尽可能优雅地降级。
5. 启用近实时监控、警报和操作控制



服务降级：

> 服务器忙碌或者网络拥堵时，不让客户端等待并立刻返回一个友好提示，fallback





**服务降级**

三种情况：

1. 程序运行异常
2. 访问超时
3. 服务熔断触发服务降级
4. 线程池 / 信号量 打满也会导致服务降级



**服务熔断**

类似保险丝达到最大服务访问后，直接拒绝



**服务限流**

高并发时，限制每秒过来多少个



服务提供者配置

新建 cloud-provider-hystrix-payment8001

导入依赖	

```
<dependencies>
    <!--hystrix-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
    </dependency>
    <!--eureka client-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <!--web-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency><!-- 引入自己定义的api通用包，可以使用Payment支付Entity -->
        <groupId>com.hzh.springcloud</groupId>
        <artifactId>cloud-api-commons</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

配置yml

```
server:
  port: 8001

spring:
  application:
    name: cloud-provider-hystrix-payment

eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      #defaultZone: http://eureka7001.com:7001/eureka,http://eureka7002.com:7002/eureka
      defaultZone: http://eureka7001.com:7001/eureka
```

service层

```java
// 节约时间，就没写接口了
@Service
public class PaymentService {

    public String paymentInfoOk(Integer id) {

        return "线程池: " + Thread.currentThread().getName() + "paymentInfo_ok,id" + id + "O(∩_∩)O";
    }

    // 模拟出错
    public String paymentInfoTimeOut(Integer id) {
        try {
            TimeUnit.SECONDS.sleep(3);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "线程池: " + Thread.currentThread().getName() + "paymentInfo_TimeOut,id" + id;
    }

}
```

controller层

```java
@Autowired
private PaymentService paymentService;
@Value("${server.port}")
private String serverPort;

@GetMapping("/payment/hystrix/ok/{id}")
public String paymentInfoOk(@PathVariable("id") Integer id){
    String result =paymentService.paymentInfoOk(id);
    log.info("****result:"+result);
    return result;
}

@GetMapping("/payment/hystrix/timeout/{id}")
public String paymentInfoTimeOut(@PathVariable("id") Integer id){
    String result =paymentService.paymentInfoTimeOut(id);
    log.info("****result:"+result);
    return result;
}
```

启动类

```java
@EnableEurekaClient
@SpringBootApplication
public class PaymentHystrixMain8001 {

    public static void main(String[] args) {
        SpringApplication.run(PaymentHystrixMain8001.class,args);

    }

}
```

启动7001和8001

用 JMeter 进行高并发测试

高并发去请求`http://localhost:8001/payment/hystrix/timeout/1`

我们去请求  `http://localhost:8001/payment/hystrix/ok/1`

结果 `http://localhost:8001/payment/hystrix/ok/1` 受到影响，明显被拖累了



新建81

cloud-consumer-feign-hystrix-order81

导入依赖

```
<dependencies>
    <!--openfeign-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    <!--eureka client-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <!-- 引入自己定义的api通用包，可以使用Payment支付Entity -->
    <dependency>
        <groupId>com.hzh.springcloud</groupId>
        <artifactId>cloud-api-commons</artifactId>
        <version>${project.version}</version>
    </dependency>
    <!--web-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <!--一般基础通用配置-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

配置yml

```yml
server:
  port: 81

eureka:
  client:
    register-with-eureka: false
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/


ribbon:
  ReadTimeout: 4000
  ConnectTimeout: 4000
```



service

```java
@Component
@FeignClient(value = "CLOUD-PROVIDER-HYSTRIX-PAYMENT")
public interface PaymentHystrixService {

    @GetMapping("/payment/hystrix/ok/{id}")
    public String paymentInfo_OK(@PathVariable("id") Integer id);

    @GetMapping("/payment/hystrix/timeout/{id}")
    public String paymentInfo_TimeOut(@PathVariable("id") Integer id);

}
```



controller

```java
@Autowired
private PaymentHystrixService paymentHystrixService;

@GetMapping("/consumer/payment/hystrix/ok/{id}")
public String paymentInfo_OK(@PathVariable("id") Integer id){
    String result =paymentHystrixService.paymentInfo_OK(id);
    return result;
}

@GetMapping("/consumer/payment/hystrix/timeout/{id}")
public String paymentInfo_TimeOut(@PathVariable("id") Integer id){
    String result =paymentHystrixService.paymentInfo_TimeOut(id);
    return result;
}
```

主启动类

```java
@SpringBootApplication
@EnableFeignClients
public class OrderHystrixMain81 {

    public static void main(String[] args) {
        SpringApplication.run(OrderHystrixMain81.class,args);

    }

}
```

启动81消费者，启动jmeter，然后再进行测试

81消费者 要么转圈圈等待，要么超时报错

因为 8001 已经处于高并发，81消费者再去调用，访问响应缓慢



**解决问题**

分析8001

​	设置自身调用超时时间的峰值,峰值内可以正常运行,超过了需要有兜底的方法处理,作服务降级 fallback



service层

```java
@Service
public class PaymentService {

    public String paymentInfoOk(Integer id) {

        return "线程池: " + Thread.currentThread().getName() + "paymentInfo_ok,id" + id + "O(∩_∩)O";
    }


    // 访问时间3秒内 就正常，超过3秒就报用 Hystrix
    @HystrixCommand(fallbackMethod = "paymentInfo_TimeOutHandler",commandProperties = {
            @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds",value="3000")
    })
    public String paymentInfoTimeOut(Integer id) {
        try {
            // 等待5秒
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "线程池: " + Thread.currentThread().getName() + "系统繁忙请稍后再试,id" + id;
    }

    public String paymentInfo_TimeOutHandler(Integer id){

        return "线程池: " + Thread.currentThread().getName() + "paymentInfo_TimeOutHandler,id" + id;
    }


}
```

启动类 加上 @EnableCircuitBreaker注解

```java
@EnableEurekaClient
@EnableCircuitBreaker
@SpringBootApplication
public class PaymentHystrixMain8001 {

    public static void main(String[] args) {
        SpringApplication.run(PaymentHystrixMain8001.class,args);

    }

}
```

对81 消费端 进行降级保护

yml

```yml
server:
  port: 81

eureka:
  client:
    register-with-eureka: false
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/

feign:
  hystrix:
    enabled: true

ribbon:
  ReadTimeout: 5000
  ConnectTimeout: 5000
```

在主启动类添加`@EnableHystrix`注解。



修改OrderHystrixController的 paymentInfo_TimeOut 方法

```java
@HystrixCommand(fallbackMethod = "paymentTimeOutFallbackMethod",commandProperties = {
        @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds",value="1500")
})
@GetMapping("/consumer/payment/hystrix/timeout/{id}")
public String paymentInfo_TimeOut(@PathVariable("id") Integer id){
    String result =paymentHystrixService.paymentInfo_TimeOut(id);
    return result;
}

public String paymentTimeOutFallbackMethod(@PathVariable("id") Integer id){

    return "消费端81，对方支付繁忙，paymentInfo_TimeOutHandler,id" + id;
}
```

测试，成功



问题  OrderHystrixController 中异常处理 耦合度过高，代码膨胀，应该分开

代码膨胀的解决办法

解决办法：设置全局fallback方法。

paymentInfo_TimeOut 改成下面的

```java
@HystrixCommand
@GetMapping("/consumer/payment/hystrix/timeout/{id}")
public String paymentInfo_TimeOut(@PathVariable("id") Integer id){
    String result =paymentHystrixService.paymentInfo_TimeOut(id);
    return result;
}

    // 下面是全局 fallback  无参数
    public String payment_Global_FallbackMethod(){

        return "全局异常处理,请稍后再尝试";
    }


```

controller 加上 @DefaultProperties(defaultFallback注解

```java
@RestController
@Slf4j
@DefaultProperties(defaultFallback = "payment_Global_FallbackMethod")
public class OrderHystrixController {

}
```



代码混乱的解决办法

新建PaymentFallbackService类，实现PaymentHystrixService接

```java
@Component
public class PaymentFallbackService implements PaymentHystrixService {

    @Override
    public String paymentInfo_OK(Integer id) {

        return "-----PaymentFallbackService fall back-paymentInfo_OK ,o(╥﹏╥)o";
    }

    @Override
    public String paymentInfo_TimeOut(Integer id) {

        return "-----PaymentFallbackService fall back-paymentInfo_TimeOut ,o(╥﹏╥)o";
    }

}
```

service   @FeignClient注解加上`fallback = PaymentFallbackService.class`属性

```java
@Component
@FeignClient(value = "CLOUD-PROVIDER-HYSTRIX-PAYMENT",fallback = PaymentFallbackService.class)
public interface PaymentHystrixService {

}
```

重新启动服务，关掉 8001 服务提供者，测试成功

此时服务端宕机了，但已经做了服务降级处理，让客户端在服务不可用时也会获得信息而不会挂起耗死服务器





### 服务熔断

**熔断机制概述**

​	熔断机制是应对雪崩效应的一种微服务链路保护机制。当扇岀链路的某个微服务岀错不可用或者响应时间太长时，会进行服务的降级，进而熔断该节点微服务的调用,快速返回错误的响应信息。

​	**当检测到该节点微服务调用响应正常后，恢复调用链路。**

​	在 Spring Cloud框架里，熔断机制通过 Hystrix 实现。 Hystrix 会监控微服务间调用的状况，当失败的调用到一定阈值，缺省是5秒内20次调用失败，就会启动熔断机制。熔断机制的注解是@ HystrixCommand



配置 服务提供方8001

修改 cloud-provider-hystrix-payment8001

service层

```java
// 服务熔断
@HystrixCommand(fallbackMethod = "paymentCircuitBreaker_fallback",commandProperties = {
        @HystrixProperty(name = "circuitBreaker.enabled",value = "true"),   // 是否开启断路器
        @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold",value = "10"),  // 请求次数
        @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds",value = "10000"), // 时间窗口期
        @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage",value = "60"),  // 60% 失败率达到多少后跳闸
})
public String paymentCircuitBreaker(@PathVariable("id") Integer id) {
    if(id < 0) {
        throw new RuntimeException("******id 不能负数");
    }
    String serialNumber = IdUtil.simpleUUID();

    return Thread.currentThread().getName()+"\t"+"调用成功，流水号: " + serialNumber;
}

public String paymentCircuitBreaker_fallback(@PathVariable("id") Integer id) {
    return "id 不能负数，请稍后再试，/(ㄒoㄒ)/~~   id: " +id;
}
```



在8001的PaymentController中添加

```java
@GetMapping("/payment/circuit/{id}")
public String paymentCircuitBreaker(@PathVariable("id") Integer id){
    String result =paymentService.paymentCircuitBreaker(id);
    log.info("****result:"+result);
    return result;
}
```



启动7001和8001 测试

如果错误率超过60%，进入熔断，熔断10秒内就算是正确的请求也返回错误信息。

10秒后再次开启半开模式，对请求进行处理，直到半开模式处理到正确请求。



总结：如果请求次数的错误率超过指定值，开启熔断，经过一段时间后，变为半开模式，然后放进一个请求进行处理，如果请求处理成功，关闭熔断；如果还是报错，继续进入熔断，再经过一段时间后，变为半开模式，再进行对下一个请求进行处理，一直在熔断，半开模式来回切换，直到请求成功，关闭熔断。



### 服务监控HystrixDashboard



新建模块cloud-consumer-hystrix-dashboard9001

pom依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

yml

```yml
server:
  port: 9001
```

主启动类 加上 @EnableHystrixDashboard 注解

```java
@EnableHystrixDashboard
@SpringBootApplication
public class HystrixDashboardMain9001 {

    public static void main(String[] args) {
        SpringApplication.run(HystrixDashboardMain9001.class,args);
    }

}
```

启动9001，在浏览器中输入 `http://localhost:9001/hystrix`

显示界面，则成功



配置服务提供者

注意：所有微服务提供者都需要在pom中引入监控依赖。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

修改8001服务提供者 的主启动类 

（@EnableCircuitBreaker 注解一定要加）

```
@EnableEurekaClient
@EnableCircuitBreaker
@SpringBootApplication
public class PaymentHystrixMain8001 {

    public static void main(String[] args) {
        SpringApplication.run(PaymentHystrixMain8001.class,args);

    }

    /**
     *此配置是为了服务监控而配置，与服务容错本身无关，springcloud升级后的坑
     *ServletRegistrationBean因为springboot的默认路径不是"/hystrix.stream"，
     *只要在自己的项目里配置上下面的servlet就可以了
     */
    @Bean
    public ServletRegistrationBean getServlet() {
        HystrixMetricsStreamServlet streamServlet = new HystrixMetricsStreamServlet();
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(streamServlet);
        registrationBean.setLoadOnStartup(1);
        registrationBean.addUrlMappings("/hystrix.stream");
        registrationBean.setName("HystrixMetricsStreamServlet");
        return registrationBean;
    }
    
}
```



启动 Eureka 7001，服务提供者 8001， 服务监控 9001

9001监控8001

输入 监控路径  `http://localhost:8001/hystrix.stream`

Delay 输入2000   表示2秒监控一次，点击 监控

在浏览器输入`http://localhost:8001/payment/circuit/1` 正常访问



多次输 `http://localhost:8001/payment/circuit/-1`  熔断器会开启

稍微等一会，然后输入正确的访问 `http://localhost:8001/payment/circuit/1` 熔断就会关闭

监控有7色，1圈，1线

可借鉴博客笔记   https://blog.csdn.net/qq_36903261/article/details/106614077 





# 十二、Gateway新一代网关



三大核心概念

**Route（路由）**



​	路由是构建网关的基本模块,它由ID，目标URI，一系列的断言和过滤器组成，如果断言为true则匹配该路由



**Predicate（断言）**

​	开发人员可以匹配HTTP请求中的所有内容(例如请求头或请求参数),如果请求与断言相匹配则进行路由



**Filter（过滤）**

​	指的是 Spring框架中 Gateway Filter的实例，使用过滤器,可以在请求被路由前或者之后对请求进行修改。



入门配置

1.新建模块cloud-gateway-gateway9527

注意事项：（坑）

一定不能加两个依赖 ，否则会报错

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```



```xml
<dependencies>
    <!--gateway-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    <!-- 引用自己定义的api通用包，可以使用Payment支付pojo -->
    <dependency>
        <groupId>com.angenin.springcloud</groupId>
        <artifactId>cloud-api-commons</artifactId>
        <version>${project.version}</version>
    </dependency>
    <!--eureka client(通过微服务名实现动态路由)-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <!--热部署-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

yml

```yml
server:
  port: 9527

spring:
  application:
    name: cloud-gateway

eureka:
  instance:
    hostname: cloud-gateway-service
  client:
    fetch-registry: true
    register-with-eureka: true
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/
```

主启动类GatewayMain9527

```java
@SpringBootApplication
@EnableEurekaClient
public class GatewayMain9527 {
    public static void main(String[] args) {
        SpringApplication.run(GatewayMain9527.class, args);
    }
}
```



开始做路由映射

两种方式配置

方式一 配置yml

```yml
server:
  port: 9527

spring:
  application:
    name: cloud-gateway
  cloud:
    gateway:
      routes:
        - id: payment_route  # 路由的id,没有规定规则但要求唯一,建议配合服务名
          #匹配后提供服务的路由地址
          uri: http://localhost:8001
          predicates:
            - Path=/payment/get/**  # 断言，路径相匹配的进行路由

        - id: payment_route2
          uri: http://localhost:8001
          predicates:
            - Path=/payment/lb/**  #断言,路径相匹配的进行路由

eureka:
  instance:
    hostname: cloud-gateway-service
  client:
    fetch-registry: true
    register-with-eureka: true
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/
```

测试，启动Eureka7001，服务提供者8001，网关9527 

`http://localhost:9527/payment/get/1 ` 能访问到 服务提供者，能隐藏8001端口



方式二

代码中注入 RouteLocator 的Bean



```java
@Configuration
public class GateWayConfig {

    /**
     * 配置了一个id为 path_route_hzh 的路由规则
     * 当访问地址 http://localhost:9527/guonei  时会自动转发到 地址 http://news.baidu.com/guonei
     * @param routeLocatorBuilder
     * @return
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder routeLocatorBuilder){
        RouteLocatorBuilder.Builder routes = routeLocatorBuilder.routes();
        routes.route("path_route_hzh",
                r-> r.path("/guonei")
                        .uri("http://news.baidu.com/guonei"));

        return routes.build();
    }
    
    @Bean
    public RouteLocator customRouteLocator2(RouteLocatorBuilder routeLocatorBuilder){
        RouteLocatorBuilder.Builder routes = routeLocatorBuilder.routes();
        routes.route("path_route_hzh",
                r-> r.path("/guoji")
                        .uri("http://news.baidu.com/guoji"));

        return routes.build();
    }
    
    


}
```

通过微服务名实现动态路由



​	默认情况下 Gateway会根据洼册中心洼册的服务列表,以注册中心上微服务名为路径创建动态路由进行转发,从而实现动态路由的功能

启动7001Eureka ，8001和8002服务提供者

修改 9527的yml

```yml
server:
  port: 9527

spring:
  application:
    name: cloud-gateway

  cloud:
    gateway:
      discovery:
        locator:
          enabled: true   # 开启从注册中心动态创建路由的功能，利用微服务名进行路由映射

      routes:
        - id: paymeny_routh      #路由的ID，没有固定规则但要求唯一，建议配合服务名
        #  uri: http://localhost:8001/   #匹配后提供服务的路由地址

          uri: lb://cloud-payment-service   # 微服务名，lb表示 使用Ribbon负载均衡
          predicates:
            - Path=/payment/get/**    # 断言，路径相匹配的进行路由

        - id: paymeny_routh2      #路由的ID，没有固定规则但要求唯一，建议配合服务名
         # uri: http://localhost:8001/   #匹配后提供服务的路由地址
          uri: lb://cloud-payment-service   # 微服务名，lb表示 使用Ribbon负载均衡

          predicates:
            - Path=/payment/lb/**   # 断言，路径相匹配的进行路由


eureka:
  instance:
    hostname: cloud-gateway-service
  client:   #服务提供者provider注册进eureka服务列表内
    service-url:
      register-with-eureka: true
      fetch-registry: true
      defaultZone: http://eureka7001.com:7001/eureka

```

启动9572，输入  `http://localhost:9527/payment/lb`，不断的刷新访问

两个服务轮流使用，达到动态路由的功能



## Predicate的使用

**常用的Route Predicate**

**Afte**

```yml
 routes:
        - id: paymeny_routh
          uri: lb://cloud-payment-service 
          predicates:
            - Path=/payment/get/**
			- After=2020-10-31T18:30:07.428+08:00[Asia/Shanghai]   # 要在 这个时间之后，请求才有效果

```

**Before/Between**

```yml
#指定时间前才能访问（Before）
- Before=2020-06-17T11:53:40.325+08:00[Asia/Shanghai]
#指定时间内才能访问（Between）
- Between=2020-06-17T11:53:40.325+08:00[Asia/Shanghai],2020-06-17T12:53:40.325+08:00[Asia/Shanghai]
```

Method

```yml
- Method=GET	#只允许get请求访问
```



Predicate 就是为了实现一组匹配规格，让请求过来找到对应的Route进行处理。



## Filter的使用

自定义过滤器

新建filter.MyLogGateWayFilter



```java
/**
 * 自定义过滤器
 *
 * 能做什么？
 * 全局日志记录
 * 统一网关鉴权
 *
 */

@Component
@Slf4j
public class MyLogGateWayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("******** come in MyLogGateWayFilter");
        String uname=exchange.getRequest().getQueryParams().getFirst("uname");
        if(uname == null){
            log.info("*****用户名为null，非法用户");
            exchange.getResponse().setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
```



启动7001，8001，8002，9527  

输入 `http://localhost:9527/payment/lb `              没网页显示

` http://localhost:9527/payment/lb?uname=10`    有网页显示



# 十三、SpringCloud Config分布式配置中心

分布式系统面临的--配置问题：

​	微服务意味着要将单体应用中的业务拆分成一个个子服务,每个服务的粒度相对较小,因此系统中会出现大量的服务。由于每个服务都需要必要的配置信息才能运行,所以一套集中式的、动态的配置管理设施是必不可少的。



SpringCloud Config是什么？

​	SpringCloud Config为微服务架构中的微服务提供集中化的外部配置支持,配置服务器为各个不同微服务应用的所有环境提供了一个中心化的外部配置。



SpringCloud Config分为 **服务端** 和 **客户端** 两部分。

​	服务端也称为**分布式配置中心**,它是个独立的微服务应用,用来连接配置服务器并为客户端提供获取配置信息,加密/解密信息等访问接口。

​	客户端则是通过指定的配置中心来管理应用资源,以及与业务相关的配置内容,并在启动的时候从配置中心获取和加载配置信息配置朓务器默认采用gt来存储配置信息,这样就有助于对环境配置进行版本管理,并且可以通过gi客户端工具来方便的管理和访问配置內容。



SpringCloud Config 能干嘛



1.集中管理配置文件

2.不同环境不同配置,动态化的配置更新,分环境部罟比如dev/ test/prod/beta/ /release

3.运行期间动态调整配置,不再需要在每个服务部罟的机器上编写配置文件,服务会向配置中心统一拉取配置自己的信息

4.当配置发生变动时,服务不需要重启即可感知到配置的变化并应用新的配置

5.将配置信息以REST接口的形式暴露



与GitHub 整合配置

​	由于SpringCloud Config默认使用Gt来存储配置文件(也有其它方式比如支持SVN和本地文件)但最推荐的还是Git,而且使用的是http:/httpsi问的形式





https://blog.csdn.net/qq_36903261/article/details/106814648

新建模块 cloud-config-center3344

依赖

```xml
<dependencies>

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-config-server</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

yml

```yml
server:
  port: 3344

spring:
  application:
    name:  cloud-config-center  #注册进Eureka服务器的微服务名
  cloud:
    config:
      server:
        git:
          uri: https://github.com/806658038/springcloud-config.git   #GitHub上面的git仓库名字（Http方式）
        ####搜索目录
          search-paths:
            - springcloud-config
      ####读取分支
      label: master


#服务注册到eureka地址
eureka:
  client:
    service-url:
      defaultZone: http://localhost:7001/eureka
```

启动类

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigCenterMain3344 {
    public static void main(String[] args) {
        SpringApplication.run(ConfigCenterMain3344.class, args);
    }

}
```

在github 新建  springcloud-config 仓库，再新建几个文件，config-dev.yml，config-prod.yml，config-test.yml



启动7001，配置中心3344

输入  `http://localhost:3344/master/config-dev.yml`

就可以得到 github中 config-dev.yml文件的内容

​	

**Config客户端配置与测试**

新建 cloud-config-client-3355

pom

```xml
<dependencies>
    <!--config server-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-config</artifactId>
    </dependency>
    <!--eureka client(通过微服务名实现动态路由)-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <!--热部署-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```



​	Bootstrap 属性有高优先级,默认情況下,它们不会被本地配置覆盖。 Bootstrap context和 Application Context有着不同的约定，所以新増了一个 bootstrap.yml 文件,保证 Bootstrap Context和 Application Context 配置的分离。



bootstrap.yml 是系统级的，优先度更高

appication.yml 是用户级的资源配置



要将 Client模块下的 application.yml 文件改为 bootstrap.yml 这是很关键的,因为 bootstrap.yml是比 application.yml先加载的。

 bootstrap.ym优先级高于application.yml



bootstrap.yml

```yml
server:
  port: 3355


spring:
  application:
    name: config-client
  cloud:
    config: #config客户端配置
      label: master   #分支名称
      name: config    #配置文件名称       这三个综合：master分支上的config-dev.yml的配置文件
      profile: dev    #读取后缀名称       被读取到http://localhost:3344/master/config/dev
      uri: http://localhost:3344  #配置中心地址


eureka:
  client:
    service-url:
      defaultZone: http://localhost:7001/eureka   #服务注册到的eureka地址
```

主启动类

```java
@EnableEurekaClient
@SpringBootApplication
public class ConfigClientMain3355 {
    
    public static void main(String[] args) {
        SpringApplication.run(ConfigClientMain3355.class, args);
    }
    
}
```

controller（读取GitHub的配置文件）

```java
@RestController
public class ConfigClientController {

    @Value("${config.info}")	//spring的@Value注解
    private String configInfo;

    @GetMapping("/configInfo")
    public String getConfigInfo(){
        return configInfo;
    }

}
```



测试，启动7001，3344，3355， 输入 `http://localhost:3355/configInfo`

和 `http://localhost:3344/master/config-dev.yml` 一致



动态刷新问题

修改	Github上的配置内容

刷新3344，发现ConfigService配置中心立即响应

刷新3355，发现ConfigService配置中心没有任何响应，需重启3355才有效





**Config客户端之动态刷新**

修改 3355项目

往3355项目 在pom中添加（上面已经加了）

```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
```

然后在bootstrap.yml中添加

```yml
#暴露监控端点
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

在ConfigClientController类上加上`@RefreshScope`注解。

```java
@RestController
@RefreshScope
public class ConfigClientController {

}
```

启动3355 测试，测试失败

问题：需要发送post请求刷新3355才能生效

打开 cmd 终端，

输入`curl -X POST "http://localhost:3355/actuator/refresh"`

刷新`http://localhost:3355/configInfo`  生效



问题

​	假如有多个微服务客户端3355/3366/3377 ...

​	每个微服务都要执行一次pos请求，麻烦

​	可否广播,一次通知,处处生效



SpringCloud Bus 消息总线 来解决这个问题
