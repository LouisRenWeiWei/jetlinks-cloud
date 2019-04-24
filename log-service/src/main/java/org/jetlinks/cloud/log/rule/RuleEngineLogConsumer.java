package org.jetlinks.cloud.log.rule;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface RuleEngineLogConsumer {

    String ruleEngineLog="rule.engine.execute.log";

    String ruleEngineDoneEvent="rule.engine.execute.event.NODE_EXECUTE_DONE";

    String ruleEngineFailEvent="rule.engine.execute.event.NODE_EXECUTE_FAIL";


    @Input(ruleEngineLog)
    SubscribableChannel ruleEngineLog();

    @Input(ruleEngineDoneEvent)
    SubscribableChannel ruleEngineDoneEvent();

    @Input(ruleEngineFailEvent)
    SubscribableChannel ruleEngineFailEvent();


}
