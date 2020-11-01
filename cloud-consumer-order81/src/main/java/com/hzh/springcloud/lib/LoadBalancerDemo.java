package com.hzh.springcloud.lib;


import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

public interface LoadBalancerDemo {

    ServiceInstance instances(List<ServiceInstance> serviceInstances);

}
