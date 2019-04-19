package org.jetlinks.cloud.rule.worker;

import lombok.SneakyThrows;
import org.jetlinks.rule.engine.api.Logger;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.Slf4jLogger;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.executor.Input;
import org.jetlinks.rule.engine.api.executor.Output;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class DataMappingWorkerNodeTest {

    @Test
    @SneakyThrows
    public void test() {
        DataMappingWorkerNode node = new DataMappingWorkerNode();

        DataMappingWorkerNode.Config config = new DataMappingWorkerNode.Config();

        config.getMappings().add(new DataMappingWorkerNode.Mapping("name", "_name"));
        config.getMappings().add(new DataMappingWorkerNode.Mapping("date", "_date", "date"));
        config.getMappings().add(new DataMappingWorkerNode.Mapping("age", "${#_age+1}", "int"));

        AtomicReference<Object> output = new AtomicReference<>();

        AtomicReference<Consumer<RuleData>> consumerReference = new AtomicReference<>();

        Function<RuleData, CompletionStage<Object>> data = node.createExecutor(new ExecutionContext() {
            @Override
            public Logger logger() {
                return new Slf4jLogger("test");
            }

            @Override
            public Input getInput() {
                return new Input() {
                    @Override
                    public void accept(Consumer<RuleData> accept) {

                    }

                    @Override
                    public boolean acceptOnce(Consumer<RuleData> accept) {
                        consumerReference.set(accept);
                        return true;
                    }

                    @Override
                    public void close() {

                    }
                };
            }

            @Override
            public Output getOutput() {
                return output::set;
            }

            @Override
            public void fireEvent(String event, RuleData data) {

            }

            @Override
            public void onError(RuleData data, Throwable e) {

            }

            @Override
            public void stop() {

            }

            @Override
            public void onStop(Runnable runnable) {

            }
        }, config);

        Map<String, Object> mapData = new HashMap<>();
        mapData.put("_name", "test");
        mapData.put("_date", "2019-01-10");
        mapData.put("_age", "10");


        Map<String, Object> result = data.apply(RuleData.create(mapData))
                .toCompletableFuture()
                .thenApply(Map.class::cast)
                .get();

        Assert.assertNotNull(result.get("name"));
        Assert.assertNotNull(result.get("age"));
        Assert.assertNotNull(result.get("date"));


    }
}