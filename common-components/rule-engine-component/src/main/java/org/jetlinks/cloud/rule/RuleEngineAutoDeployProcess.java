package org.jetlinks.cloud.rule;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.rule.engine.api.Rule;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.cluster.RunMode;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.persistent.RuleInstancePersistent;
import org.jetlinks.rule.engine.api.persistent.RulePersistent;
import org.jetlinks.rule.engine.api.persistent.repository.RuleInstanceRepository;
import org.jetlinks.rule.engine.api.persistent.repository.RuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "jetlinks.rule-engine.deploy")
@Getter
@Setter
public class RuleEngineAutoDeployProcess implements CommandLineRunner {

    private String path = "classpath:/rule-engine/*.xml";

    private boolean enabled = true;

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private RuleEngineModelParser parser;

    @Autowired
    private RuleInstanceRepository repository;

    @Autowired
    private RuleRepository ruleRepository;

    @Override
    public void run(String... args) throws Exception {
        if (!enabled) {
            return;
        }
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(path);
        for (Resource resource : resources) {
            InputStream inputStream = resource.getInputStream();
            String data = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            RuleModel model = parser.parse("re.xml", data);
            Rule rule = new Rule();
            rule.setId(model.getId());
            rule.setVersion(1);
            rule.setModel(model);

            RulePersistent persistent = new RulePersistent();
            persistent.setId(rule.getId());
            persistent.setRuleId(rule.getId());
            persistent.setModelFormat("re.xml");
            persistent.setModel(data);
            persistent.setName(resource.getFilename());
            persistent.setVersion(1);

            ruleRepository.save(persistent);
            RuleInstancePersistent instancePersistent = repository.findInstanceByRuleId(rule.getId())
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (null == instancePersistent) {
                ruleEngine.startRule(rule);
            } else {
                ruleEngine.getInstance(instancePersistent.getId());
            }
        }

    }
}
