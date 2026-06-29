package com.cgs.smartclassbackendpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 支付服务启动类
 *
 * <p>修复：</p>
 * <ul>
 *   <li>添加 {@code @EnableDiscoveryClient}，使 pay 服务能注册到 Nacos 并被发现</li>
 *   <li>添加 {@code @EnableFeignClients}，使 pay 服务能通过 Feign 调用其他微服务（如 user 服务激活 VIP）</li>
 * </ul>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.cgs.smartclassbackendserviceclient.service"})
public class SmartclassBackendPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartclassBackendPayApplication.class, args);
    }

}
