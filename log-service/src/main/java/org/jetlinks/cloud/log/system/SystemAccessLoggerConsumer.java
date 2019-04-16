package org.jetlinks.cloud.log.system;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface SystemAccessLoggerConsumer {

    String systemLogger = "system.logger";
    String accessLogger = "access.logger";

    @Input("system.logger")
    SubscribableChannel systemLogger();

    @Input("access.logger")
    SubscribableChannel accessLogger();

}
