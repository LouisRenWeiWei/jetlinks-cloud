package org.jetlinks.cloud.device.manager.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceOnlineOfflineEvent {
    private String deviceId;

    private long timestamp;

    private boolean offline;

}
