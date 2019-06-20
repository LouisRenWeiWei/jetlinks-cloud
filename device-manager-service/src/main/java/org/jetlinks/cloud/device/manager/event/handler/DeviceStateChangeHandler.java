package org.jetlinks.cloud.device.manager.event.handler;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.cloud.device.manager.event.DeviceOnlineOfflineEvent;
import org.jetlinks.cloud.device.manager.service.LocalDeviceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;


@Component
@Slf4j
public class DeviceStateChangeHandler implements CommandLineRunner {


    @Autowired
    private LocalDeviceInstanceService deviceInstanceService;

    @Value("${device.state.sync.buffer-size:500}")
    private int bufferSize = 500;

    @Value("${device.state.sync.buffer-timeout:2}")
    private int bufferTimeout = 2;

    private volatile FluxSink<String> sink;

    @EventListener
    public void handleDeviceOnlineOfflineEvent(DeviceOnlineOfflineEvent event) {
        if (StringUtils.hasText(event.getDeviceId())) {
            if (sink == null) {
                initInputConsumer();
            }
            if (sink != null) {
                sink.next(event.getDeviceId());
            }
        } else {
            log.warn("错误的设备上下线消息:{}", JSON.toJSONString(event));
        }
    }


    private synchronized void initInputConsumer() {
        if (sink == null) {
            Flux.<String>create(fluxSink -> this.sink = fluxSink)
                    .bufferTimeout(bufferSize, Duration.ofSeconds(2))
                    .subscribe(list -> deviceInstanceService.syncState(list, false));
        }
    }

    @Override
    public void run(String... strings) {
        initInputConsumer();
    }

}
