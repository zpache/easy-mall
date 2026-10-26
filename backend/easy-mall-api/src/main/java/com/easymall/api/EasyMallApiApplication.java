package com.easymall.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * easy-mall 小程序端接口服务启动类。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EasyMallApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyMallApiApplication.class, args);
    }
}
