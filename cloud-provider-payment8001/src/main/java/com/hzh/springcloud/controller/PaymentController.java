package com.hzh.springcloud.controller;

import com.hzh.springcloud.pojo.CommonResult;
import com.hzh.springcloud.pojo.Payment;
import com.hzh.springcloud.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;
    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private DiscoveryClient discoveryClient;    // 获取想要的服务信息

    // 一定要加@RequestBody，否则 consumer 操作时，插入的数据会为null
    @PostMapping(value = "/payment/create")
    public CommonResult create(@RequestBody Payment payment){
        int result = paymentService.create(payment);
        log.info("***插入结果:"+result);
        if(result>0){
            return new CommonResult(200,"插入数据库成功,serverPort:"+serverPort,result);
        }else{
            return new CommonResult(444,"插入数据库失败",null);
        }
    }

    @GetMapping(value = "/payment/get/{id}")
    public CommonResult getPaymentById(@PathVariable("id") Long id){
        Payment payment = paymentService.getPaymentById(id);
        log.info("***获取结果:"+payment);
        if(payment!=null){
            return new CommonResult(200,"查询成功,"+serverPort,payment);
        }else{
            return new CommonResult(444,"没有对应的记录,查询ID"+id,null);
        }
    }

    // 获取服务信息
    @GetMapping("/payment/discovery")
    public Object discovery(){
        List<String> services =discoveryClient.getServices();
        for (String s :services){
            log.info("*****s:"+s);
        }

        List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
        for (ServiceInstance instance:instances){
            log.info(instance.getServiceId()+"\t"+instance.getHost()+"\t"+instance.getPort()+"\t"+instance.getUri());
        }
        return this.discoveryClient;
    }

    // 测试自己实现的轮询算法
    @GetMapping(value = "/payment/lb")
    public String getPaymentLB() {
        return serverPort;
    }

    // 模拟超时
    @GetMapping(value = "/payment/feign/timeout")
    public String paymentFeignTimeout() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return serverPort;
    }

    public void demo(){}

    public void demo2(){

    }


}
