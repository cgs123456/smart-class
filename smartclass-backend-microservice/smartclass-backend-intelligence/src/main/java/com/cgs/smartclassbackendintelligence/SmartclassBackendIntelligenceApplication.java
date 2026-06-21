package com.cgs.smartclassbackendintelligence;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.cgs.smartclassbackendserviceclient.service"})
@MapperScan("com.cgs.smartclassbackendintelligence.mapper")
@SpringBootApplication
public class SmartclassBackendIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartclassBackendIntelligenceApplication.class, args);
    }

}
