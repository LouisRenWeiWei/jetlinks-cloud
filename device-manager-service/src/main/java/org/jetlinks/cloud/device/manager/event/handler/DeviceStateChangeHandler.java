package org.jetlinks.cloud.device.manager.event.handler;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.task.batch.local.LocalBatchTaskBuilderFactory;
import org.hswebframework.task.batch.local.LocalCacheBatching;
import org.jetlinks.cloud.device.manager.event.DeviceOnlineOfflineEvent;
import org.jetlinks.cloud.device.manager.service.LocalDeviceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.function.Consumer;


@Component
@Slf4j
public class DeviceStateChangeHandler implements CommandLineRunner {

    private LocalBatchTaskBuilderFactory factory = new LocalBatchTaskBuilderFactory();

    @Autowired
    private LocalDeviceInstanceService deviceInstanceService;

    @Value("${device.state.sync.batch-size:200}")
    private int batchSize = 200;

    private volatile Consumer<String> input;

    @EventListener
    public void handleDeviceOnlineOfflineEvent(DeviceOnlineOfflineEvent event) {
        if (StringUtils.hasText(event.getDeviceId())) {
            input.accept(event.getDeviceId());
        } else {
            log.warn("错误的设备上下线消息:{}", JSON.toJSONString(event));
        }
    }

    @Override
    public void run(String... strings) {
        factory.<String, Integer>create()
                .input(input -> DeviceStateChangeHandler.this.input = input)
                .batching(new LocalCacheBatching<>(getBatchSize()))//批量缓冲
                .handle((list, output) -> output.write(deviceInstanceService.syncState(list, false)))
                .build()
                .output(total -> log.debug("同步设备状态数量:{}", total))
                .start();
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
