package org.jetlinks.cloud.rule.worker;

import jdk.nashorn.internal.runtime.Undefined;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.SkipNextValue;
import org.jetlinks.rule.engine.executor.supports.RuleNodeConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@Component
@Slf4j
public class ScriptWorkerNode extends CommonExecutableRuleNodeFactoryStrategy<ScriptWorkerNode.Config> {

    @Override
    public ScriptWorkerNode.Config newConfigInstance() {
        return new Config();
    }

    @Override
    public String getSupportType() {
        return "script";
    }

    @Override
    @SneakyThrows
    public Function<RuleData, CompletionStage<Object>> createExecutor(ExecutionContext context, ScriptWorkerNode.Config config) {

        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine(config.getLang());
        if (engine == null) {
            throw new UnsupportedOperationException("不支持的脚本语言:" + config.getLang());
        }
        String id = DigestUtils.md5Hex(config.getScript());
        if (!engine.compiled(id)) {
            engine.compile(id, config.getScript());
        }

        Handler handler = new Handler();
        Map<String, Object> scriptContext = new HashMap<>();
        scriptContext.put("context", context);
        scriptContext.put("handler", handler);
        engine.execute(id, scriptContext).getIfSuccess();

        return ruleData -> {
            if (handler.onMessage != null) {
                Object result = handler.onMessage.apply(ruleData);
                if (result == null || result instanceof Undefined) {
                    return CompletableFuture.completedFuture(SkipNextValue.INSTANCE);
                }

                return CompletableFuture.completedFuture(result);
            }
            //原路返回
            return CompletableFuture.completedFuture(ruleData);

        };
    }

    public static class Handler {
        private Function<RuleData, Object> onMessage;

        public void onMessage(Function<RuleData, Object> onMessage) {
            this.onMessage = onMessage;
        }
    }

    @Getter
    @Setter
    public static class Config implements RuleNodeConfig {

        private String lang = "js";

        private String script;

        private NodeType nodeType;

    }
}
