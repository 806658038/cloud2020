package com.hzh.springcloud.service;

import com.hzh.springcloud.pojo.Payment;

public interface PaymentService {

    int create(Payment payment);

    Payment getPaymentById(Long id);

}
