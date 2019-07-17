package org.jetlinks.cloud.rule;

import org.jetlinks.cloud.redis.LettuceClientRepository;
import org.jetlinks.cloud.rule.repository.LettuceRuleInstanceRepository;
import org.jetlinks.cloud.rule.repository.LettuceRuleRepository;
import org.jetlinks.rule.engine.api.ConditionEvaluator;
import org.jetlinks.rule.engine.api.cluster.ClusterManager;
import org.jetlinks.rule.engine.api.cluster.WorkerNodeSelector;
import org.jetlinks.rule.engine.api.cluster.ha.HaManager;
import org.jetlinks.rule.engine.api.executor.ExecutableRuleNodeFactory;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.api.persistent.repository.RuleInstanceRepository;
import org.jetlinks.rule.engine.api.persistent.repository.RuleRepository;
import org.jetlinks.rule.engine.cluster.DefaultWorkerNodeSelector;
import org.jetlinks.rule.engine.cluster.WorkerNodeSelectorStrategy;
import org.jetlinks.rule.engine.cluster.lettuce.LettuceClusterManager;
import org.jetlinks.rule.engine.cluster.lettuce.LettuceHaManager;
import org.jetlinks.rule.engine.cluster.scheduler.DefaultRuleEngineScheduler;
import org.jetlinks.rule.engine.cluster.worker.RuleEngineWorker;
import org.jetlinks.rule.engine.condition.ConditionEvaluatorStrategy;
import org.jetlinks.rule.engine.condition.DefaultConditionEvaluator;
import org.jetlinks.rule.engine.condition.supports.DefaultScriptEvaluator;
import org.jetlinks.rule.engine.condition.supports.ScriptConditionEvaluatorStrategy;
import org.jetlinks.rule.engine.condition.supports.ScriptEvaluator;
import org.jetlinks.rule.engine.executor.DefaultExecutableRuleNodeFactory;
import org.jetlinks.rule.engine.executor.ExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.model.DefaultRuleModelParser;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;
import org.jetlinks.rule.engine.model.xml.XmlRuleModelParserStrategy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Configuration
public class RuleEngineConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "jetlinks.rule-engine.cluster")
    public RuleEngineProperties ruleEngineProperties(Environment environment) {
        RuleEngineProperties properties = new RuleEngineProperties();
        properties.init(environment);
        return properties;
    }

    @Bean
    @ConditionalOnMissingBean(RuleRepository.class)
    public LettuceRuleRepository lettuceRuleRepository(LettuceClientRepository repository, RuleEngineProperties properties){
        return new LettuceRuleRepository(properties.getName(),repository.getClientOrDefault(properties.getRedisName()));

    }

    @Bean
    @ConditionalOnMissingBean(RuleInstanceRepository.class)
    public LettuceRuleInstanceRepository lettuceRuleInstanceRepository(LettuceClientRepository repository, RuleEngineProperties properties){
        return new LettuceRuleInstanceRepository(properties.getName(),repository.getClientOrDefault(properties.getRedisName()));

    }
//    @Bean
//    @ConditionalOnMissingBean(RuleRepository.class)
//    public RedissonRuleRepository redissonRuleRepository(RedissonClientRepository repository,
//                                                         RuleEngineProperties properties) {
//        return new RedissonRuleRepository(
//                properties.getName(),
//                getRedissonClient(repository, properties)
//        );
//    }
//
//    @Bean
//    @ConditionalOnMissingBean(RuleInstanceRepository.class)
//    public RedissonRuleInstanceRepository redissonRuleInstanceRepository(RedissonClientRepository repository,
//                                                                         RuleEngineProperties properties) {
//        return new RedissonRuleInstanceRepository(
//                properties.getName(),
//                getRedissonClient(repository, properties)
//        );
//    }

//    private RedissonClient getRedissonClient(RedissonClientRepository repository,
//                                             RuleEngineProperties properties) {
//        return repository.getClient(properties.getRedisName())
//                .orElseGet(repository::getDefaultClient);
//    }

    @Bean
    public XmlRuleModelParserStrategy xmlRuleModelParserStrategy() {
        return new XmlRuleModelParserStrategy();
    }

    @Bean
    public DefaultRuleModelParser defaultRuleModelParser() {
        return new DefaultRuleModelParser();
    }

    @Bean
    public DefaultConditionEvaluator defaultConditionEvaluator() {
        return new DefaultConditionEvaluator();
    }

    @Bean
    public DefaultExecutableRuleNodeFactory defaultExecutableRuleNodeFactory() {
        return new DefaultExecutableRuleNodeFactory();
    }

    @Bean
    public DefaultWorkerNodeSelector defaultWorkerNodeSelector() {
        return new DefaultWorkerNodeSelector();
    }

    @Bean
    public BeanPostProcessor autoRegisterStrategy(DefaultRuleModelParser defaultRuleModelParser,
                                                  DefaultConditionEvaluator defaultConditionEvaluator,
                                                  DefaultExecutableRuleNodeFactory ruleNodeFactory,
                                                  DefaultWorkerNodeSelector workerNodeSelector) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof RuleModelParserStrategy) {
                    defaultRuleModelParser.register(((RuleModelParserStrategy) bean));
                }
                if (bean instanceof ConditionEvaluatorStrategy) {
                    defaultConditionEvaluator.register(((ConditionEvaluatorStrategy) bean));
                }
                if (bean instanceof ExecutableRuleNodeFactoryStrategy) {
                    ruleNodeFactory.registerStrategy(((ExecutableRuleNodeFactoryStrategy) bean));
                }
                if (bean instanceof WorkerNodeSelectorStrategy) {
                    workerNodeSelector.register(((WorkerNodeSelectorStrategy) bean));
                }
                return bean;
            }
        };
    }

    @Bean
    public ScriptEvaluator ruleEngineScriptEvaluator() {
        return new DefaultScriptEvaluator();
    }

    @Bean
    public ScriptConditionEvaluatorStrategy scriptConditionEvaluatorStrategy(ScriptEvaluator scriptEvaluator) {
        return new ScriptConditionEvaluatorStrategy(scriptEvaluator);
    }

//    @Bean(initMethod = "start", destroyMethod = "shutdown")
//    public RedissonHaManager ruleEngineHaManager(RedissonClientRepository repository,
//                                                 ScheduledExecutorService executorService,
//                                                 RuleEngineProperties properties) {
//        RedissonHaManager redissonHaManager = new RedissonHaManager();
//        redissonHaManager.setClusterName(properties.getName());
//        redissonHaManager.setRedissonClient(getRedissonClient(repository, properties));
//        redissonHaManager.setExecutorService(executorService);
//        redissonHaManager.setCurrentNode(properties.toNodeInfo());
//        return redissonHaManager;
//
//    }

    @Bean(initMethod = "startup",destroyMethod = "shutdown")
    public LettuceHaManager lettuceHaManager(LettuceClientRepository lettuceClientRepository, RuleEngineProperties properties) {

        return new LettuceHaManager(properties.toNodeInfo(), lettuceClientRepository.getClientOrDefault("rule-engine").getHaManager(properties.getName()));
//        return manager;
    }

    @Bean
    public LettuceClusterManager clusterManager(LettuceClientRepository lettuceClientRepository, HaManager haManager, RuleEngineProperties properties) {
        LettuceClusterManager clusterManager = new LettuceClusterManager(lettuceClientRepository.getClientOrDefault("rule-engine"));
        clusterManager.setHaManager(haManager);
        clusterManager.setName(properties.getName());
        return clusterManager;
    }

//    @Bean(initMethod = "start", destroyMethod = "shutdown")
//    public RedissonClusterManager ruleEngineClusterManager(RedissonClientRepository repository,
//                                                           HaManager haManager,
//                                                           ScheduledExecutorService executorService,
//                                                           RuleEngineProperties properties) {
//        RedissonClusterManager clusterManager = new RedissonClusterManager();
//        clusterManager.setName(properties.getName());
//        clusterManager.setRedissonClient(getRedissonClient(repository, properties));
//        clusterManager.setHaManager(haManager);
//        clusterManager.setExecutorService(executorService);
//        return clusterManager;
//    }

    @Bean(initMethod = "start")
    public DefaultRuleEngineScheduler ruleEngineScheduler(ClusterManager clusterManager,
                                                          RuleEngineModelParser modelParser,
                                                          RuleRepository ruleRepository,
                                                          WorkerNodeSelector workerNodeSelector,
                                                          RuleInstanceRepository instanceRepository) {
        DefaultRuleEngineScheduler engine = new DefaultRuleEngineScheduler();
        engine.setClusterManager(clusterManager);
        engine.setRuleRepository(ruleRepository);
        engine.setInstanceRepository(instanceRepository);
        engine.setModelParser(modelParser);
        engine.setNodeSelector(workerNodeSelector);
        return engine;
    }


    @Bean(initMethod = "start")
    public RuleEngineWorker ruleEngineWorker(ClusterManager clusterManager,
                                             RuleEngineModelParser modelParser,
                                             RuleRepository ruleRepository,
                                             ConditionEvaluator conditionEvaluator,
                                             ApplicationEventPublisher eventPublisher,
                                             ExecutableRuleNodeFactory ruleNodeFactory) {
        RuleEngineWorker worker = new RuleEngineWorker();
        worker.setClusterManager(clusterManager);
        worker.setRuleRepository(ruleRepository);
        worker.setModelParser(modelParser);
        worker.setNodeFactory(ruleNodeFactory);
        worker.setConditionEvaluator(conditionEvaluator);
        worker.setExecuteEventConsumer(eventPublisher::publishEvent);
        worker.setExecuteLogEventConsumer(eventPublisher::publishEvent);

        return worker;
    }

}
