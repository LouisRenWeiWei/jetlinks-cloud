package org.jetlinks.cloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.EnableZuulServer;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@SpringCloudApplication
@EnableZuulServer
@EnableZuulProxy
public class DashboardGateWayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardGateWayApplication.class, args);
    }

}
