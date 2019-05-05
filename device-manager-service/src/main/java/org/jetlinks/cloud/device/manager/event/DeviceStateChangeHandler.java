package org.jetlinks.cloud.device.manager.event;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.task.batch.local.LocalBatchTaskBuilderFactory;
import org.hswebframework.task.batch.local.LocalCacheBatching;
import org.jetlinks.cloud.device.manager.service.LocalDeviceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.Optional.*;

@Component
@EnableBinding
@Slf4j
public class DeviceStateChangeHandler implements CommandLineRunner {

    @Autowired
    private BinderAwareChannelResolver resolver;

    @Autowired
    private  BindingService bindingService;

    private LocalBatchTaskBuilderFactory factory = new LocalBatchTaskBuilderFactory();

    @Autowired
    private LocalDeviceInstanceService deviceInstanceService;

    public String getDeviceId(Message message) {
        Object payload = message.getPayload();
        if (payload instanceof byte[]) {
            payload = new String(((byte[]) payload));
        }
        if (payload instanceof String) {
            payload = JSON.parseObject(((String) payload));
        }
        if (payload instanceof Map) {
            return (String) ((Map) payload).get("deviceId");
        }
        return null;
    }

    @Override
    public void run(String... strings) {
        SubscribableChannel connect = (SubscribableChannel) resolver.resolveDestination("device.connect");
        SubscribableChannel disConnect = (SubscribableChannel) resolver.resolveDestination("device.disconnect");
        bindingService.bindConsumer(connect,"device.connect");
        bindingService.bindConsumer(disConnect,"device.disconnect");
        factory.<String, Integer>create()
                .input(input -> {
                    connect.subscribe(message -> ofNullable(getDeviceId(message)).ifPresent(input));
                    disConnect.subscribe(message -> ofNullable(getDeviceId(message)).ifPresent(input));
                })
                .batching(new LocalCacheBatching<>(200)) //每200个为一批
                .handle((list, output) -> output.write(deviceInstanceService.syncState(list, false)))
                .build()
                .output(total -> log.debug("同步设备状态数量:{}", total))
                .start();
    }
}
