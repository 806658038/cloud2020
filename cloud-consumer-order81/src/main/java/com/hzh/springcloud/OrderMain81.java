package com.hzh.springcloud;

import com.hzh.myrule.MyRule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;

// 必须是大写
// @RibbonClient(name ="CLOUD-PAYMENT-SERVICE",configuration = MyRule.class)
@EnableEurekaClient
@SpringBootApplication
public class OrderMain81 {

    public static void main(String[] args) {
        SpringApplication.run(OrderMain81.class,args);

    }

}
