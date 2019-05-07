package org.jetlinks.cloud.device.manager.event.dispatcher;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface DeviceOnlineOfflineConsumer {

    String deviceOnline = "device.connect";

    String deviceOffline = "device.disconnect";

    @Input(deviceOnline)
    SubscribableChannel deviceOnline();

    @Input(deviceOffline)
    SubscribableChannel deviceOffline();
}
