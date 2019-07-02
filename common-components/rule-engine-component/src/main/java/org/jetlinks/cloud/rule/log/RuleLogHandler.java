package org.jetlinks.cloud.rule.log;

import com.alibaba.fastjson.JSON;
import org.jetlinks.rule.engine.api.events.NodeExecuteEvent;
import org.jetlinks.rule.engine.api.events.RuleEvent;
import org.jetlinks.rule.engine.cluster.worker.NodeExecuteLogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.event.EventListener;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@EnableBinding
@ConditionalOnBean(BinderAwareChannelResolver.class)
public class RuleLogHandler {

    @Autowired
    private BinderAwareChannelResolver resolver;

    @EventListener
    @Async
    @Retryable
    public void handleRuleLog(NodeExecuteLogEvent event) {
        resolver.resolveDestination("rule.engine.execute.log")
                .send(MessageBuilder.withPayload(JSON.toJSONString(event.getLogInfo())).build(),
                        TimeUnit.SECONDS.toMillis(5));
    }

    @EventListener
    @Async
    @Retryable
    public void handleRuleExecuteEvent(NodeExecuteEvent executeEvent) {
        if (!RuleEvent.NODE_EXECUTE_BEFORE.equals(executeEvent.getEvent())) {
            resolver.resolveDestination("rule.engine.execute.event." + executeEvent.getEvent())
                    .send(MessageBuilder.withPayload(JSON.toJSONString(executeEvent)).build(),
                            TimeUnit.SECONDS.toMillis(5));
        }
    }
}
