package org.jetlinks.cloud.rule.worker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.cloud.redis.RedissonClientRepository;
import org.jetlinks.cloud.rule.RuleEngineProperties;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.cluster.ha.HaManager;
import org.jetlinks.rule.engine.api.executor.ExecutableRuleNode;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.executor.RuleNodeConfiguration;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.AbstractExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.supports.RuleNodeConfig;
import org.redisson.api.CronSchedule;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
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
    private RedissonClientRepository redissonClientRepository;

    @Autowired
    private RuleEngineProperties ruleEngineProperties;

    private RedissonClient redissonClient;

    private Map<String, TimerInfo> contexts = new ConcurrentHashMap<>();

    private RMap<String, String> timerServerInfo;

    @Override
    public Function<RuleData, CompletionStage<Object>> createExecutor(ExecutionContext context, Config config) {

        throw new UnsupportedOperationException();
    }

    @PostConstruct
    public void start() {

        redissonClient = redissonClientRepository.getClient(ruleEngineProperties.getRedisName())
                .orElseGet(redissonClientRepository::getDefaultClient);

        timerServerInfo = redissonClient.getMap("rule-engine-timer-info", StringCodec.INSTANCE);
        //有节点退出了.需要接管此节点上的任务
        haManager.onNodeLeave(nodeInfo -> {

            RSet<String> rules = redissonClient.getSet("scheduler-timer-info:".concat(nodeInfo.getId()));
            for (String rule : rules) {
                tryInitAndRunTimer(rule, true);
            }
        });

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

        //当前节点的调度记录
        RSet<String> history = redissonClient.getSet("scheduler-timer-info:".concat(haManager.getCurrentNode().getId()));
        String serverNodeId = haManager.getCurrentNode().getId();

        //直接由当前节点进行调度
        if (history.contains(config.getRuleId()) || force) {
            timerServerInfo.put(config.getRuleId(), haManager.getCurrentNode().getId());
        } else {
            //尝试使用当前节点进行调度,和其他节点一起竞争任务.
            serverNodeId = timerServerInfo.putIfAbsent(config.getRuleId(), haManager.getCurrentNode().getId());

        }
        String scheduleServerId = serverNodeId == null ? haManager.getCurrentNode().getId() : serverNodeId;

        redissonClient.getSet("scheduler-timer-info:".concat(scheduleServerId)).add(config.getRuleId());

        //当前节点
        if (haManager.getCurrentNode().getId().equals(scheduleServerId)) {
            runTimer(config.getRuleId());
        }
    }

    @Override
    @SneakyThrows
    protected ExecutableRuleNode doCreate(Config config) {
        config.validate();

        return executionContext -> {
            contexts.put(config.getRuleId(), new TimerInfo(executionContext, config, false));

            executionContext.onStop(() -> contexts.remove(config.getRuleId()));

            tryInitAndRunTimer(config.getRuleId(), false);

            executionContext.getInput()
                    .acceptOnce(ruleData -> executionContext.getOutput().write(ruleData));
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
        executorService.schedule(() -> {
            TimerInfo timerInfo = contexts.get(id);
            if (timerInfo != null) {
                if (executorService.isShutdown()) {
                    return;
                }
                String serverId = haManager.getCurrentNode().getId();

                if (!serverId.equals(timerServerInfo.get(timerInfo.config.getRuleId()))) {
                    log.debug("定时调度[{}]已在此节点[{}]取消", timerInfo.config.getRuleId(), serverId);
                    info.runningLocal = false;
                    return;
                }
                if (log.isInfoEnabled()) {
                    log.info("触发定时任务[{}]:{}", id, timerInfo.getConfig().dataJson);
                }
                timerInfo.context.getOutput()
                        .write(RuleData.create(timerInfo.getConfig().dataJson));

                //下一个周期
                runTimer(id);
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
