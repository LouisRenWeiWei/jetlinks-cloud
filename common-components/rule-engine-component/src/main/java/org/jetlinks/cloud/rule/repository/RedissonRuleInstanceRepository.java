package org.jetlinks.cloud.rule.repository;

import lombok.AllArgsConstructor;
import org.jetlinks.rule.engine.api.RuleInstanceState;
import org.jetlinks.rule.engine.api.persistent.RuleInstancePersistent;
import org.jetlinks.rule.engine.api.persistent.repository.RuleInstanceRepository;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
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
    public List<RuleInstancePersistent> findAll() {
        return new ArrayList<>(redissonClient.<String, RuleInstancePersistent>getMap(prefixName + ":rule:instance:repo").values());
    }

    @Override
    public List<RuleInstancePersistent> findBySchedulerId(String schedulerId) {
        return findAll()
                .stream()
                .filter(persistent -> schedulerId.equals(persistent.getCurrentSchedulerId()) || schedulerId.equals(persistent.getSchedulerId()))
                .collect(Collectors.toList());
    }

    @Override
    public void saveInstance(RuleInstancePersistent instancePersistent) {
        redissonClient.<String, RuleInstancePersistent>getMap(prefixName + ":rule:instance:repo")
                .fastPut(instancePersistent.getId(), instancePersistent);
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
