package org.jetlinks.cloud.influx;

import org.influxdb.InfluxDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Component
@EnableConfigurationProperties(MultiInfluxProperties.class)
public class DefaultInfluxDBRepository implements InfluxDBRepository {

    @Autowired
    private MultiInfluxProperties properties;

    private Map<String, InfluxDB> repository = new HashMap<>();

    @PostConstruct
    public void init() {
        properties.getClients().forEach((name, conf) -> repository.put(name, conf.create()));
    }

    @Override
    public Optional<InfluxDB> getInfluxDB(String name) {
        return Optional.ofNullable(repository.get(name));
    }
}
