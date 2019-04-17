package org.jetlinks.cloud.rule.repository;

import lombok.AllArgsConstructor;
import org.jetlinks.rule.engine.api.persistent.RuleInstancePersistent;
import org.jetlinks.rule.engine.api.persistent.repository.RuleInstanceRepository;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@AllArgsConstructor
public class RedissonRuleInstanceRepository implements RuleInstanceRepository {

    private String prefixName;

    private RedissonClient redissonClient;

    @Override
    public Optional<RuleInstancePersistent> findInstanceById(String instanceId) {
        return Optional.ofNullable(
                redissonClient.<String, RuleInstancePersistent>getMap(prefixName + ":rule:instance:repo")
                        .get(instanceId)
        );
    }

    @Override
    public List<RuleInstancePersistent> findInstanceByRuleId(String ruleId) {
        return redissonClient.<String, RuleInstancePersistent>getMap(prefixName + ":rule:instance:repo")
                .values()
                .stream()
                .filter(persistent -> persistent.getRuleId().equals(ruleId))
                .collect(Collectors.toList());
    }

    @Override
    public void saveInstance(RuleInstancePersistent instancePersistent) {
        redissonClient.<String, RuleInstancePersistent>getMap(prefixName + ":rule:instance:repo")
                .fastPut(instancePersistent.getId(), instancePersistent);
    }

    @Override
    public void stopInstance(String instanceId) {
        findInstanceById(instanceId)
                .ifPresent(p -> {
                    p.setRunning(false);
                    saveInstance(p);
                });
    }

    @Override
    public void startInstance(String instanceId) {
        findInstanceById(instanceId)
                .ifPresent(p -> {
                    p.setRunning(true);
                    saveInstance(p);
                });
    }
}
