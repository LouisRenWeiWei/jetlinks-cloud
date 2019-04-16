package org.jetlinks.cloud.influx;

import org.influxdb.InfluxDB;

import java.util.Optional;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface InfluxDBRepository {

    Optional<InfluxDB> getInfluxDB(String name);

    default InfluxDB getDefault() {
        return getInfluxDB("default").orElseThrow(() -> new IllegalStateException("default influxdb not found"));
    }

}
