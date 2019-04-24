package org.jetlinks.cloud.log.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

@Component
@EnableBinding({RuleEngineLogConsumer.class})
@Slf4j
public class RuleEngineLogHandler {

    @StreamListener(RuleEngineLogConsumer.ruleEngineLog)
    public void handleLogEvent(String logJson) {
        log.debug("规则引擎执行日志:{}", logJson);
    }

    @StreamListener(RuleEngineLogConsumer.ruleEngineFailEvent)
    public void handleFailEvent(String eventData) {
        log.debug("规则引擎执行失败:{}", eventData);
    }

    @StreamListener(RuleEngineLogConsumer.ruleEngineDoneEvent)
    public void handleDoneEvent(String eventData) {
        log.debug("规则引擎执行完成:{}", eventData);
    }

}
