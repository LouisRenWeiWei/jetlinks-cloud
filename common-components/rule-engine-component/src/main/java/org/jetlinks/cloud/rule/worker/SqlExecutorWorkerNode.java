package org.jetlinks.cloud.rule.worker;

import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.executor.SqlExecutor;
import org.hswebframework.ezorm.rdb.meta.expand.SimpleMapWrapper;
import org.hswebframework.web.datasource.DataSourceHolder;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.SkipNextValue;
import org.jetlinks.rule.engine.executor.supports.RuleNodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Component
public class SqlExecutorWorkerNode extends CommonExecutableRuleNodeFactoryStrategy<SqlExecutorWorkerNode.Config> {

    @Autowired
    private SqlExecutor sqlExecutor;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public Config newConfigInstance() {
        return new Config();
    }

    @Override
    public String getSupportType() {
        return "sql";
    }

    @Override
    public Function<RuleData, CompletionStage<Object>> createExecutor(ExecutionContext context, Config config) {

        if (config.isQuery()) {
            return data -> Try.of(() -> {
                config.switchDataSource();
                try {
                    List<Map<String, Object>> all = new ArrayList<>();
                    data.acceptMap(map ->
                            Try.run(() -> all.addAll(sqlExecutor.list(config.getSql(), map, new SimpleMapWrapper() {
                                @Override
                                public boolean done(Map<String, Object> instance) {
                                    if (config.stream) {
                                        context.getOutput()
                                                .write(data.newData(instance));
                                        return false;
                                    }
                                    return super.done(instance);
                                }
                            }))));
                    if (!config.stream) {
                        return all;
                    } else {
                        return SkipNextValue.INSTANCE;
                    }

                } finally {
                    config.resetDataSource();
                }
            }).toCompletableFuture();
        } else {
            return data ->
                    Try.<Object>of(() -> {
                        config.switchDataSource();
                        try {
                            AtomicInteger total = new AtomicInteger();
                            if (config.isTransaction()) {
                                //使用事务的时候,在事务内去调用output.write,实现下游事务
                                return transactionTemplate.execute(transactionStatus -> {
                                    data.acceptMap((map) -> Try.run(() -> total.addAndGet(sqlExecutor.update(config.getSql(), map))).get());
                                    if (config.getNodeType().isReturnNewValue()) {
                                        context.getOutput().write(data.newData(total.get()));
                                    } else {
                                        context.getOutput().write(data.copy());
                                    }
                                    return SkipNextValue.INSTANCE;
                                });

                            } else {
                                data.acceptMap((map) -> Try.run(() -> total.addAndGet(sqlExecutor.update(config.getSql(), map))).get());
                            }
                            return total.get();
                        } finally {
                            config.resetDataSource();
                        }
                    }).toCompletableFuture();
        }

    }


    @Getter
    @Setter
    public static class Config implements RuleNodeConfig {

        private String dataSourceId;

        private NodeType nodeType;

        private String sql;

        private boolean stream;

        private boolean transaction;

        public boolean isQuery() {

            return sql.trim().startsWith("SELECT") ||
                    sql.trim().startsWith("select");
        }

        public void switchDataSource() {
            if (dataSourceId != null) {
                DataSourceHolder.switcher().use(dataSourceId);
            }
        }

        public void resetDataSource() {
            if (dataSourceId != null) {
                DataSourceHolder.switcher().useLast();
            }
        }
    }

}
