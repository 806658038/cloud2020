package com.hzh.springcloud.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
