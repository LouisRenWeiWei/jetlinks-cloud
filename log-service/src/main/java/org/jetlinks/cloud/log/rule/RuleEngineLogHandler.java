package org.jetlinks.cloud.log.rule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.rule.engine.api.DefaultRuleData;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.events.NodeExecuteEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.jetlinks.rule.engine.api.RuleDataHelper.*;

@Component
@EnableBinding({RuleEngineLogConsumer.class})
@Slf4j
public class RuleEngineLogHandler {

    @Autowired
    private JestClient jestClient;

    @StreamListener(RuleEngineLogConsumer.ruleEngineLog)
    public void handleLogEvent(String logJson) {
        Bulk.Builder bulkBuilder = new Bulk.Builder();
        JSONObject jsonObject = JSONObject.parseObject(logJson);

        Index index = new Index.Builder(jsonObject)
                .id(IDGenerator.MD5.generate())
                .index("jetlinks-rule-engine-log")
                .type("rule-engine-log")
                .build();
        bulkBuilder.addAction(index);

        jestClient.executeAsync(bulkBuilder.build(), new JestResultHandler<BulkResult>() {
            @Override
            public void completed(BulkResult result) {
                if (!result.isSucceeded()) {
                    log.error("保存规则引擎日志失败:\n{}", result.getJsonString());
                }
            }

            @Override
            public void failed(Exception e) {
                log.error("保存规则引擎日志失败", e);
            }
        });
    }

    @StreamListener(RuleEngineLogConsumer.ruleEngineFailEvent)
    public void handleFailEvent(String eventData) {
        handleNodeEvent(parseEvent(eventData));
    }

    private NodeExecuteEvent parseEvent(String json) {
        NodeExecuteEvent executeEvent = new NodeExecuteEvent();
        JSONObject jsonObject = JSON.parseObject(json);

        FastBeanCopier.copy(jsonObject, executeEvent, "ruleData");
        executeEvent.setRuleData(jsonObject.getJSONObject("ruleData").toJavaObject(DefaultRuleData.class));

        handleNodeEvent(executeEvent);
        return executeEvent;
    }

    @StreamListener(RuleEngineLogConsumer.ruleEngineDoneEvent)
    public void handleDoneEvent(String eventData) {
        handleNodeEvent(parseEvent(eventData));
    }

    @SneakyThrows
    public void handleNodeEvent(NodeExecuteEvent event) {
        JSONObject indexData = new JSONObject();

        RuleData ruleData = event.getRuleData();
        indexData.put("dataId", event.getRuleData().getId());
        indexData.put("instanceId", event.getInstanceId());
        indexData.put("nodeId", event.getNodeId());
        indexData.put("event", event.getEvent());
        indexData.put("ruleData", JSON.toJSONString(ruleData));

        ruleData.getAttribute(EXECUTE_TIME)
                .ifPresent(time -> indexData.put("executeTime", time));

        if (hasError(ruleData)) {
            indexData.put("errorType", ruleData.getAttribute(ERROR_TYPE).orElse(null));
            indexData.put("errorMessage", ruleData.getAttribute(ERROR_MESSAGE).orElse(null));
            indexData.put("errorStack", ruleData.getAttribute(ERROR_STACK).orElse(null));
            RuleDataHelper.clearError(ruleData);
        }

        Bulk.Builder bulkBuilder = new Bulk.Builder();

        Index index = new Index.Builder(indexData)
                .id(IDGenerator.MD5.generate())
                .index("jetlinks-rule-engine-event")
                .type("rule-engine-event")
                .build();

        bulkBuilder.addAction(index);

        jestClient.executeAsync(bulkBuilder.build(), new JestResultHandler<BulkResult>() {
            @Override
            public void completed(BulkResult result) {
                if (!result.isSucceeded()) {
                    log.error("记录规则引擎事件失败:{}", result.getJsonString());
                }
            }

            @Override
            public void failed(Exception e) {
                log.error("记录规则引擎事件失败", e);
            }
        });


    }
}
