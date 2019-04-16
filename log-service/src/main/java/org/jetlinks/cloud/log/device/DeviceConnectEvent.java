package org.jetlinks.cloud.log.device;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class DeviceConnectEvent implements Serializable {

    private String deviceId;

    private long timestamp;
}
