package com.easymall.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * easy-mall 管理后台接口服务启动类。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EasyMallAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyMallAdminApplication.class, args);
    }
}
