package org.jetlinks.cloud.rule.repository;

import io.lettuce.core.api.StatefulRedisConnection;
import lombok.SneakyThrows;
import org.jetlinks.lettuce.LettucePlus;
import org.jetlinks.rule.engine.api.RuleInstanceState;
import org.jetlinks.rule.engine.api.persistent.RuleInstancePersistent;
import org.jetlinks.rule.engine.api.persistent.repository.RuleInstanceRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LettuceRuleInstanceRepository implements RuleInstanceRepository {

    private String redisKey;

    private LettucePlus plus;

    public LettuceRuleInstanceRepository(String prefix, LettucePlus plus) {
        this.redisKey = prefix.concat(":rule:instance:repo");
        this.plus = plus;
    }

    @Override
    @SneakyThrows
    public Optional<RuleInstancePersistent> findInstanceById(String instanceId) {

        return plus.<String, RuleInstancePersistent>getConnection()
                .thenApply(StatefulRedisConnection::async)
                .thenCompose(redis -> redis.hget(redisKey, instanceId))
                .thenApply(Optional::ofNullable)
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
    }

    @Override
    @SneakyThrows
    public List<RuleInstancePersistent> findInstanceByRuleId(String ruleId) {

        return plus.<String, RuleInstancePersistent>getConnection()
                .thenApply(StatefulRedisConnection::async)
                .thenCompose(redis -> redis.hvals(redisKey))
                .thenApply(list -> list.stream().filter(instance -> instance.getRuleId().equals(ruleId)).collect(Collectors.toList()))
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
    }

    @Override
    @SneakyThrows
    public List<RuleInstancePersistent> findAll() {
        return plus.<String, RuleInstancePersistent>getConnection()
                .thenApply(StatefulRedisConnection::async)
                .thenCompose(redis -> redis.hvals(redisKey))
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
    }

    @Override
    public List<RuleInstancePersistent> findBySchedulerId(String schedulerId) {
        return findAll()
                .stream()
                .filter(persistent -> schedulerId.equals(persistent.getCurrentSchedulerId()) || schedulerId.equals(persistent.getSchedulerId()))
                .collect(Collectors.toList());
    }

    @Override
    @SneakyThrows
    public void saveInstance(RuleInstancePersistent instancePersistent) {

        plus.<String, RuleInstancePersistent>getConnection()
                .thenApply(StatefulRedisConnection::async)
                .thenAccept(redis -> redis.hset(redisKey, instancePersistent.getId(), instancePersistent))
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
    }

    @Override
    public void changeState(String instanceId, RuleInstanceState state) {
        findInstanceById(instanceId)
                .map(r -> {
                    r.setState(state);
                    return r;
                })
                .ifPresent(this::saveInstance);
    }
}
