package org.jetlinks.cloud.influx;

import lombok.Getter;
import lombok.Setter;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.util.Assert;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class InfluxProperties {

    private String url;

    private String username;

    private String password;

    private String database;

    public InfluxDB create() {
        Assert.hasText(url,"influx.url");
        Assert.hasText(database,"influx.database");
        Assert.hasText(username,"influx.username");
        Assert.hasText(password,"influx.password");

        InfluxDB influxDB = InfluxDBFactory.connect(url, username, password);
        influxDB.setDatabase(database);
        return influxDB;
    }
}
