package org.jetlinks.cloud.rule;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetlinks.rule.engine.api.cluster.NodeInfo;
import org.jetlinks.rule.engine.api.cluster.NodeRole;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.net.InetAddress;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class RuleEngineProperties {

    // ${spring.application.name}+${host}
    private String nodeId;

    private String name = "default";

    private NodeRole[] rules = {NodeRole.WORKER, NodeRole.SCHEDULER, NodeRole.MONITOR};

    private String[] tags;

    private String redisName = "rule-engine";

    public boolean hasRule(NodeRole rule) {
        return rule.in(rules);
    }

    public NodeInfo toNodeInfo() {
        Assert.hasText(name, "cluster.name");
        Assert.hasText(nodeId, "cluster.nodeId");
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setId(nodeId);
        nodeInfo.setRoles(rules);
        nodeInfo.setTags(tags);
        nodeInfo.setName(name);
        return nodeInfo;
    }

    public void init(Environment environment) {
        this.nodeId = environment
                .getProperty("spring.application.name")
                + ":" + environment.getProperty("HOSTNAME"
                , environment.getProperty("HOST", getHost()));
    }

    @SneakyThrows
    public String getHost() {
        InetAddress address = InetAddress.getLocalHost();
        return address.getHostName() + "/" + address.getHostAddress();
    }
}
