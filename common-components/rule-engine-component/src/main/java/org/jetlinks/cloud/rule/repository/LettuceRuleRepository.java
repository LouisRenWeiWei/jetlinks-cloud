package org.jetlinks.cloud.rule.repository;

import io.lettuce.core.api.StatefulRedisConnection;
import lombok.SneakyThrows;
import org.jetlinks.lettuce.LettucePlus;
import org.jetlinks.rule.engine.api.persistent.RulePersistent;
import org.jetlinks.rule.engine.api.persistent.repository.RuleRepository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LettuceRuleRepository implements RuleRepository {


    private String redisKey;

    private LettucePlus lettucePlus;

    public LettuceRuleRepository(String prefixName, LettucePlus plus) {
        this.lettucePlus = plus;
        this.redisKey = prefixName.concat(":rule:repo");
    }

    @Override
    @SneakyThrows
    public Optional<RulePersistent> findRuleById(String ruleId) {

        return lettucePlus.<String, RulePersistent>getConnection()
                .thenApply(StatefulRedisConnection::async)
                .thenCompose(redis -> redis.hget(redisKey, ruleId))
                .thenApply(Optional::ofNullable)
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
    }

    @Override
    @SneakyThrows
    public List<RulePersistent> findRuleByIdList(Collection<String> ruleIdList) {
        return lettucePlus.<Object, RulePersistent>getConnection()
                .thenApply(StatefulRedisConnection::async)
                .thenCompose(redis -> redis.hmget(redisKey, ruleIdList.toArray()))
                .thenApply(list->list.stream().map(kv->kv.getValueOrElse(null)).filter(Objects::nonNull).collect(Collectors.toList()))
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
    }

    @Override
    @SneakyThrows
    public void save(RulePersistent persistent) {

        lettucePlus.<String, RulePersistent>getConnection()
                .thenApply(StatefulRedisConnection::async)
                .thenAccept(redis -> redis.hset(redisKey, persistent.getId(), persistent))
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
    }
}
