package org.jetlinks.cloud.device.manager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringCloudApplication
@ComponentScan("org.jetlinks.cloud")
@EnableCaching
@MapperScan("org.jetlinks.cloud.device.manager.dao")
public class DeviceManagerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeviceManagerServiceApplication.class, args);
    }
}
