package org.jetlinks.cloud.redis.lettuce;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties("jetlinks.redis")
public class MultiLettuceProperties {

    private Map<String, LettuceProperties> clients = new HashMap<>();

    private int threadSize = Runtime.getRuntime().availableProcessors() * 2;


}
