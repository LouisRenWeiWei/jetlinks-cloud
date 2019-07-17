package org.jetlinks.cloud.rule.repository;

import lombok.AllArgsConstructor;
import org.jetlinks.rule.engine.api.persistent.RulePersistent;
import org.jetlinks.rule.engine.api.persistent.repository.RuleRepository;
import org.redisson.api.RedissonClient;

import java.util.*;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@AllArgsConstructor
public class RedissonRuleRepository implements RuleRepository {
    private String prefixName;

    private RedissonClient redissonClient;

    @Override
    public Optional<RulePersistent> findRuleById(String ruleId) {
        return Optional.ofNullable(redissonClient.<String, RulePersistent>getMap(prefixName + ":rule:repo").get(ruleId));
    }

    @Override
    public List<RulePersistent> findRuleByIdList(Collection<String> ruleIdList) {
        return new ArrayList<>(redissonClient.<String, RulePersistent>getMap(prefixName + ":rule:repo").values());
    }

    @Override
    public void save(RulePersistent persistent) {
        redissonClient.<String, RulePersistent>getMap(prefixName + ":rule:repo")
                .fastPut(persistent.getId(), persistent);
    }
}
