package org.jetlinks.cloud.log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@SpringCloudApplication
@ComponentScan("org.jetlinks.cloud")
public class LogServiceApplication {

    public static void main(String[] args) {
        System.out.println(JSON.toJSONString(System.getenv(), SerializerFeature.PrettyFormat));

        SpringApplication.run(LogServiceApplication.class, args);
    }
}
