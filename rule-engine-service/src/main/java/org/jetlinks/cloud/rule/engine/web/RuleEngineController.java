package org.jetlinks.cloud.rule.engine.web;

import lombok.SneakyThrows;
import org.hswebframework.web.controller.message.ResponseMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/rule-engine")
public class RuleEngineController {

    @Autowired
    private RuleEngine engine;

    @PostMapping("/{instanceId}/stop")
    @SneakyThrows
    public ResponseMessage<Object> stop(@PathVariable String instanceId) {

        engine.getInstance(instanceId).stop();
        return ResponseMessage.ok();
    }

    @PostMapping("/{instanceId}/start")
    @SneakyThrows
    public ResponseMessage<Object> start(@PathVariable String instanceId) {

        engine.getInstance(instanceId).start();
        return ResponseMessage.ok();
    }

    @PostMapping("/{instanceId}/{startWith}/to/{endWith}")
    @SneakyThrows
    public ResponseMessage<Object> execute(@PathVariable String instanceId,
                                           @PathVariable String startWith,
                                           @PathVariable String endWith,
                                           @RequestBody Map<String, Object> payload) {

        RuleData ruleData = RuleData.create(payload);
        RuleDataHelper.markSyncReturn(ruleData, endWith);
        RuleDataHelper.markStartWith(ruleData, startWith);

        return ResponseMessage.ok(engine.getInstance(instanceId)
                .execute(ruleData)
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS)
                .getData());
    }

}
