package org.jetlinks.cloud.logging;

import org.hswebframework.web.logging.AccessLoggerInfo;
import org.hswebframework.web.logging.events.AccessLoggerAfterEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "logging.stream.producer", name = "enabled", havingValue = "true",matchIfMissing = true)
@EnableBinding
public class LoggingHandler {

    @Autowired
    private BinderAwareChannelResolver resolver;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private Environment environment;

    @EventListener
    public void handleSystemLogger(SystemLoggerInfo logInfo) {
        resolver.resolveDestination("system.logger")
                .send(MessageBuilder.withPayload(logInfo).build());
    }

    @EventListener
    public void handleAccessLogger(AccessLoggerAfterEvent event) {
        AccessLoggerInfo loggerInfo = event.getLogger();
        resolver.resolveDestination("access.logger")
                .send(MessageBuilder.withPayload(SerializableAccessLoggerInfo.of(loggerInfo)).build());
    }

}
