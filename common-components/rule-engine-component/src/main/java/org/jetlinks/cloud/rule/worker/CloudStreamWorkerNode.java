package org.jetlinks.cloud.rule.worker;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.rule.engine.api.Logger;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.executor.ExecutableRuleNode;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.AbstractExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.supports.RuleNodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.messaging.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Component
@EnableBinding
@ConditionalOnClass(BinderAwareChannelResolver.class)
@Slf4j
public class CloudStreamWorkerNode extends AbstractExecutableRuleNodeFactoryStrategy<CloudStreamWorkerNode.Config> {

    @Autowired
    private BinderAwareChannelResolver resolver;

    @Autowired
    private BindingService bindingService;

    @Override
    public Config newConfig() {
        return new Config();
    }

    @Override
    public String getSupportType() {
        return "cloud-stream";
    }

    @Override
    public Function<RuleData, CompletionStage<Object>> createExecutor(ExecutionContext context, Config config) {

        throw new UnsupportedOperationException();
    }

    private Object convertPayload(Object payload) {
        if (payload instanceof byte[]) {
            payload = new String((byte[]) payload);
        }
        if (payload instanceof String) {
            String stringPayload = ((String) payload);
            if (stringPayload.startsWith("[")) {
                return JSON.parseArray(stringPayload);
            } else if (stringPayload.startsWith("{")) {
                return JSON.parseObject(stringPayload);
            }
        }
        return payload;
    }

    @Override
    protected ExecutableRuleNode doCreate(Config config) {
        MessageChannel messageChannel = resolver.resolveDestination(config.getTopic());

        AtomicBoolean started = new AtomicBoolean();
        return context -> {
            if (started.get()) {
                return;
            }
            log.info("start spring cloud stream {} : {}", config.getType(), config.getTopic());
            started.set(true);
            if (config.type == Type.Consumer && messageChannel instanceof SubscribableChannel) {
                SubscribableChannel channel = ((SubscribableChannel) messageChannel);
                MessageHandler handler = message ->
                        context.getOutput()
                                .write(RuleData.create(convertPayload(message.getPayload())));

                channel.subscribe(handler);

                context.getInput()
                        .acceptOnce(ruleData -> context.getOutput()
                                .write(ruleData.newData(ruleData.getData())));

                context.onStop(() ->{
                    bindingService.unbindConsumers(config.getTopic());
                    channel.unsubscribe(handler);
                });
                bindingService.bindConsumer(messageChannel,config.getTopic());
            } else {
                context.getInput()
                        .acceptOnce(ruleData -> {
                            messageChannel.send(MessageBuilder.withPayload(ruleData.getData()).build());
                            context.getOutput()
                                    .write(ruleData.newData(ruleData.getData()));
                        });
            }
        };
    }

    public enum Type {
        Consumer, Producer
    }

    @Getter
    @Setter
    public static class Config implements RuleNodeConfig {

        private String topic;

        private Type type = Type.Consumer;

        @Override
        public NodeType getNodeType() {
            return NodeType.PEEK;
        }

        @Override
        public void setNodeType(NodeType nodeType) {

        }
    }
}
