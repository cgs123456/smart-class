package com.cgs.smartclassbackendstats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
// P2-5: 补充服务发现与 Feign 客户端注解（与 user/pay/course 等服务保持一致）
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.cgs.smartclassbackendserviceclient.service"})
public class SmartclassBackendStatsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartclassBackendStatsApplication.class, args);
    }

}
