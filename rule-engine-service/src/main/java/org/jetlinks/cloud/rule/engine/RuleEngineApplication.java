package org.jetlinks.cloud.rule.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@SpringCloudApplication
@ComponentScan("org.jetlinks.cloud")
public class RuleEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuleEngineApplication.class);
    }
}
