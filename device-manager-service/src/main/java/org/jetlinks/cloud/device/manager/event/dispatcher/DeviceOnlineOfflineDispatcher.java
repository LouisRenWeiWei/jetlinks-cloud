package org.jetlinks.cloud.device.manager.event.dispatcher;

import org.jetlinks.cloud.device.manager.event.DeviceOnlineOfflineEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(DeviceOnlineOfflineConsumer.class)
public class DeviceOnlineOfflineDispatcher {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @StreamListener(DeviceOnlineOfflineConsumer.deviceOnline)
    public void handleDeviceOnline(DeviceOnlineOfflineEvent event) {
        event.setOffline(false);
        eventPublisher.publishEvent(event);
    }

    @StreamListener(DeviceOnlineOfflineConsumer.deviceOffline)
    public void handleDeviceOffline(DeviceOnlineOfflineEvent event) {
        event.setOffline(true);
        eventPublisher.publishEvent(event);
    }

}
