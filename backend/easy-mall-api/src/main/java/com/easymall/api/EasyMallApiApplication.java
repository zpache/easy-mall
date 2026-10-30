package com.easymall.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * easy-mall 小程序端接口服务启动类。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
// 定时任务：订单超时关单等（见 modules/order/task）
@EnableScheduling
public class EasyMallApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyMallApiApplication.class, args);
    }
}
