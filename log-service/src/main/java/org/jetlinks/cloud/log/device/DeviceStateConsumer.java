package org.jetlinks.cloud.log.device;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceStateConsumer {
    String deviceConnect    = "device.connect";
    String deviceDisconnect = "device.disconnect";
    String deviceEvents     = "device.events";

    @Input(deviceConnect)
    SubscribableChannel deviceConnect();

    @Input(deviceDisconnect)
    SubscribableChannel deviceDisconnect();

    @Input(deviceEvents)
    SubscribableChannel deviceEvents();


}
