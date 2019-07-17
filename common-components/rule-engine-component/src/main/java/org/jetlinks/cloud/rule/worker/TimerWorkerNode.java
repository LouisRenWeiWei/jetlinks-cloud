package org.jetlinks.cloud.rule.worker;

import io.lettuce.core.api.async.RedisAsyncCommands;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.cloud.redis.LettuceClientRepository;
import org.jetlinks.cloud.rule.RuleEngineProperties;
import org.jetlinks.lettuce.LettucePlus;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.cluster.ha.HaManager;
import org.jetlinks.rule.engine.api.events.RuleEvent;
import org.jetlinks.rule.engine.api.executor.ExecutableRuleNode;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.executor.RuleNodeConfiguration;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.AbstractExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.supports.RuleNodeConfig;
import org.redisson.api.CronSchedule;
import org.redisson.executor.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@Slf4j
public class TimerWorkerNode extends AbstractExecutableRuleNodeFactoryStrategy<TimerWorkerNode.Config> {

    @Override
    public Config newConfig() {
        return new Config();
    }

    @Override
    public String getSupportType() {
        return "timer";
    }

    @Autowired
    private ScheduledExecutorService executorService;

    @Autowired
    private HaManager haManager;

    @Autowired
    private LettuceClientRepository clientRepository;

    @Autowired
    private RuleEngineProperties ruleEngineProperties;

    private Map<String, TimerInfo> contexts = new ConcurrentHashMap<>();

    @Override
    public Function<RuleData, CompletionStage<Object>> createExecutor(ExecutionContext context, Config config) {

        throw new UnsupportedOperationException();
    }

    private RedisAsyncCommands<String, String> redis;

    @PostConstruct
    @SneakyThrows
    public void start() {

        LettucePlus lettucePlus = clientRepository.getClientOrDefault(ruleEngineProperties.getRedisName());
        redis = lettucePlus.<String, String>getRedisAsync(org.jetlinks.lettuce.codec.StringCodec.getInstance())
                .toCompletableFuture().get(10, TimeUnit.SECONDS);

        //有节点退出了.需要接管此节点上的任务
        haManager.onNodeLeave(nodeInfo ->
                redis.smembers(rule -> tryInitAndRunTimer(rule, true), "scheduler-timer-info:".concat(nodeInfo.getId())));

    }

    @AllArgsConstructor
    @Getter
    private class TimerInfo {
        ExecutionContext context;
        Config config;
        volatile boolean runningLocal;
    }

    private void tryInitAndRunTimer(String id, boolean force) {
        TimerInfo info = contexts.get(id);

        if (info == null || info.runningLocal) {
            return;
        }
        Config config = info.getConfig();

        redis.smembers("scheduler-timer-info:".concat(haManager.getCurrentNode().getId()))
                .thenAccept(history -> {
                    String serverNodeId = haManager.getCurrentNode().getId();

                    //直接由当前节点进行调度
                    if (history.contains(config.getRuleId()) || force) {
                        redis.hset("timer-server-info", config.getRuleId(), serverNodeId);
                        redis.sadd("scheduler-timer-info:".concat(serverNodeId), config.getRuleId());
                        //当前节点
                        runTimer(config.getRuleId());
                    } else {
                        //尝试使用当前节点进行调度,和其他节点一起竞争任务.
                        redis.hsetnx("timer-server-info", config.getRuleId(), serverNodeId)
                                .thenAccept(success -> {
                                    if (success) {
                                        redis.sadd("scheduler-timer-info:".concat(serverNodeId), config.getRuleId());
                                        runTimer(config.getRuleId());
                                    }
                                });
                    }
                });

    }

    @Override
    @SneakyThrows
    protected ExecutableRuleNode doCreate(Config config) {
        config.validate();

        return executionContext -> {
            contexts.put(config.getRuleId(), new TimerInfo(executionContext, config, false));

            executionContext.getInput()
                    .accept(ruleData -> executionContext.getOutput().write(ruleData));

            executionContext.onStop(() -> contexts.remove(config.getRuleId()));

            tryInitAndRunTimer(config.getRuleId(), false);


        };
    }

    protected void runTimer(String id) {

        TimerInfo info = contexts.get(id);

        if (info == null) {
            return;
        }
        info.runningLocal = true;

        long delay = info.getConfig().getDelayMillis();

        log.debug("{}ms后执行定时调度:{}", delay, id);
        RuleData beforeData = RuleData.create(info.getConfig().dataJson);
        beforeData.setAttribute("timer-fire-ts", System.currentTimeMillis() + delay);

        info.context.fireEvent(RuleEvent.NODE_EXECUTE_BEFORE, beforeData);
        executorService.schedule(() -> {
            TimerInfo timerInfo = contexts.get(id);
            if (timerInfo != null) {
                if (executorService.isShutdown()) {
                    return;
                }
                String serverId = haManager.getCurrentNode().getId();
                redis.hget("timer-server-info", timerInfo.config.getRuleId())
                        .whenComplete((nil, error) -> {
                            if (null != error) {
                                log.error("获取调度信息失败", error);
                                runTimer(id);
                            }
                        })
                        .thenAccept(schedulerServer -> {
                            if (!serverId.equals(schedulerServer)) {
                                log.debug("定时调度[{}]已在此节点[{}]取消", timerInfo.config.getRuleId(), serverId);
                                info.runningLocal = false;
                                return;
                            }
                            timerInfo.context.logger()
                                    .info("触发定时任务[{}]:{}", id, timerInfo.getConfig().dataJson);

                            RuleData data = RuleData.create(timerInfo.getConfig().dataJson);

                            timerInfo.context.getOutput().write(data);
                            timerInfo.context.fireEvent(RuleEvent.NODE_EXECUTE_DONE, data);

                            //下一个周期
                            runTimer(id);
                        });
            }

        }, delay, TimeUnit.MILLISECONDS);

    }

    @Override
    public Config convertConfig(RuleNodeConfiguration configuration) {
        Config config = super.convertConfig(configuration);
        config.setRuleId(configuration.getId());

        return config;
    }

    @Getter
    @Setter
    public static class Config implements RuleNodeConfig {

        private String ruleId;

        private NodeType nodeType;

        private String cron;

        private Long rate;

        private String dataJson;

        @SneakyThrows
        public void validate() {
            if (rate == null) {
                CronExpression.validateExpression(cron);
            }
        }

        public long getDelayMillis() {
            return rate != null ? rate : CronSchedule.of(getCron())
                    .getExpression()
                    .getTimeAfter(new Date())
                    .getTime() - System.currentTimeMillis();
        }

    }
}
