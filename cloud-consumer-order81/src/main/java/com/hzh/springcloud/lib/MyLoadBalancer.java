package com.hzh.springcloud.lib;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
