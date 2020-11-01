package com.hzh.springcloud.controller;

import com.hzh.springcloud.pojo.CommonResult;
import com.hzh.springcloud.pojo.Payment;
import com.hzh.springcloud.service.PaymentFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OrderController {
    @Autowired
    private PaymentFeignService paymentFeignService;

    @GetMapping(value = "/consumer/payment/get/{id}")
    public CommonResult<Payment> getPaymentById(@PathVariable("id") Long id){

        return paymentFeignService.getPaymentById(id);
    }

    // 模拟超时
    @GetMapping(value = "/consumer/payment/feign/timeout")
    public String paymentFeignTimeout() {
        // openFeign ribbon 客户端 默认等待1秒钟
        return paymentFeignService.paymentFeignTimeout();
    }


}
