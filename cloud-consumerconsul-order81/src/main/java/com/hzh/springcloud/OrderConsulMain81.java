package com.hzh.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @auther zzyy
 * @create 2020-02-19 16:22
 */
@SpringBootApplication
@EnableDiscoveryClient  //该注解用于向使用consul或者zookeeper作为注册中心时注册服务
public class OrderConsulMain81 {

    public static void main(String[] args) {
            SpringApplication.run(OrderConsulMain81.class, args);
    }

}
