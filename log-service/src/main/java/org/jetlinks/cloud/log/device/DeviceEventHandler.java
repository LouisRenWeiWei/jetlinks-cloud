package org.jetlinks.cloud.log.device;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.utils.time.DateFormatter;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.jetlinks.cloud.influx.InfluxDBRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 设备事件处理
 *
 * @author zhouhao
 * @since 1.0.0
 */
@Component
@EnableBinding(DeviceStateConsumer.class)
@Slf4j
public class DeviceEventHandler {

    @Autowired
    private InfluxDBRepository repositry;

    private InfluxDB influxDB;

    @PostConstruct
    public void init() {
        influxDB = repositry.getInfluxDB("device-monitor")
                .orElseGet(repositry::getDefault);
    }

    @StreamListener(DeviceStateConsumer.deviceConnect)
    public void handleDeviceOnline(DeviceConnectEvent event) {
        if (log.isInfoEnabled()) {
            log.info("设备[{}]连接:{}", event.getDeviceId(), DateFormatter.toString(new Date(event.getTimestamp()), "yyyy-MM-dd HH:mm:ss"));
        }

        Point point = Point.measurement("device_connect")
                .time(event.getTimestamp(), TimeUnit.MILLISECONDS)
                .addField("deviceId", event.getDeviceId())
                .build();

        influxDB.write(point);

    }

    @StreamListener(DeviceStateConsumer.deviceDisconnect)
    public void handleDeviceOnline(DeviceDisconnectEvent event) {
        if (log.isInfoEnabled()) {
            log.info("设备[{}]断开连接:{}", event.getDeviceId(), DateFormatter.toString(new Date(event.getTimestamp()), "yyyy-MM-dd HH:mm:ss"));
        }

        Point point = Point.measurement("device_disconnect")
                .time(event.getTimestamp(), TimeUnit.MILLISECONDS)
                .addField("deviceId", event.getDeviceId())
                .build();

        influxDB.write(point);
    }

    @StreamListener(DeviceStateConsumer.deviceEvents)
    public void handleDeviceOnline(String event) {
        if (log.isInfoEnabled()) {
            log.info("设备事件:{}", event);
        }
    }

}
