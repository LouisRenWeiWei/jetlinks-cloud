package org.jetlinks.cloud.influx;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jetlinks.influx")
public class MultiInfluxProperties {

    private Map<String, InfluxProperties> clients = new HashMap<>();


}
