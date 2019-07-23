package org.jetlinks.cloud.rule.worker;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hswebframework.web.ExpressionUtils;
import org.jetlinks.core.device.DeviceOperation;
import org.jetlinks.core.device.registry.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.supports.RuleNodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@Component
@ConditionalOnBean(DeviceRegistry.class)
public class DeviceMessageWorkerNode extends CommonExecutableRuleNodeFactoryStrategy<DeviceMessageWorkerNode.Config> {


    @Autowired
    private DeviceRegistry deviceRegistry;

    @Override
    public Config newConfigInstance() {
        return new Config();
    }

    @Override
    public String getSupportType() {
        return "send-device-message";
    }

    @Override
    public Function<RuleData, CompletionStage<Object>> createExecutor(ExecutionContext context, Config config) {

        return ruleData -> {
            CompletableFuture<Object> future = new CompletableFuture<>();
            try {
                ruleData.acceptMap(data -> {
                    String deviceId = config.getRealDeviceId(data);

                    DeviceOperation operation = deviceRegistry.getDevice(deviceId);
                    if (operation == null) {
                        future.completeExceptionally(new NullPointerException("设备[" + deviceId + "]不存在"));
                        return;
                    }
                    config.send(operation, data)
                            .whenComplete((reply, throwable) -> {
                                if (throwable != null) {
                                    future.completeExceptionally(throwable);
                                } else {
                                    future.complete(reply);
                                }
                            });
                });

            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
            return future;
        };
    }

    @Getter
    @Setter
    public static class Config implements RuleNodeConfig {

        private String deviceId;

        private String message;

        private Map<String, String> parameter;

        private NodeType nodeType;

        private MessageType messageType;

        private String function;

        private List<String> properties;

        private int timeoutSeconds = 15;

        @SneakyThrows
        private String getRealDeviceId(Map<String, Object> ruleData) {
            String id = (String) ruleData.get(deviceId);
            if (id != null) {
                return id;
            }
            return ExpressionUtils.analytical(deviceId, ruleData, "spel");

        }

        private CompletionStage<? extends DeviceMessageReply> send(DeviceOperation operation, Map<String, Object> ruleData) {
            return messageType.doSend(this, operation, ruleData);
        }
    }

    public enum MessageType {
        invokeMethod {
            @Override
            CompletionStage<? extends DeviceMessageReply> doSend(Config config, DeviceOperation operation, Map<String, Object> ruleData) {
                return operation.messageSender()
                        .invokeFunction(config.getFunction())
                        .setParameter(ruleData)
                        .timeout(config.getTimeoutSeconds())
                        .send();
            }
        }, readProperty {
            @Override
            CompletionStage<? extends DeviceMessageReply> doSend(Config config, DeviceOperation operation, Map<String, Object> ruleData) {
                return operation.messageSender()
                        .readProperty()
                        .read(config.getProperties())
                        .timeout(config.getTimeoutSeconds())
                        .send();
            }
        };

        abstract CompletionStage<? extends DeviceMessageReply> doSend(Config config, DeviceOperation operation, Map<String, Object> ruleData);
    }
}
