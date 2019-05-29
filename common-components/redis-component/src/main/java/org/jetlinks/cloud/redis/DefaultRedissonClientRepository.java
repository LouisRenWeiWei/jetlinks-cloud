package org.jetlinks.cloud.redis;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhouhao
 * @since 1.1.0
 */
@Slf4j
public class DefaultRedissonClientRepository implements RedissonClientRepository, Ordered {

    @Autowired
    private MultiRedissonProperties multiRedissonProperties;

    private Map<String, RedissonClient> repository = new HashMap<>();

    private EventLoopGroup eventLoopGroup;

    @Getter
    @Setter
    private ExecutorService executorService;

    @PreDestroy
    public void destroy() {
        for (RedissonClient client : repository.values()) {
            log.debug("shutdown redisson {}", client);
            client.shutdown();
        }
    }

    @PostConstruct
    @SneakyThrows
    public void init() {
        TransportMode transportMode = multiRedissonProperties.getTransportMode();
        int threadSize = multiRedissonProperties.getThreadSize();
        if (transportMode == TransportMode.EPOLL) {
            eventLoopGroup = new EpollEventLoopGroup(threadSize, new DefaultThreadFactory("redisson-epoll-netty"));
        } else if (transportMode == TransportMode.KQUEUE) {
            eventLoopGroup = new KQueueEventLoopGroup(threadSize, new DefaultThreadFactory("redisson-kqueue-netty"));
        } else if (transportMode == TransportMode.NIO) {
            eventLoopGroup = new NioEventLoopGroup(threadSize, new DefaultThreadFactory("redisson-nio-netty"));
        }
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(threadSize, new DefaultThreadFactory("redisson"));
        }

        for (Map.Entry<String, RedissonProperties> entry : multiRedissonProperties.getClients().entrySet()) {
            Config config = entry.getValue().toConfig(multiRedissonProperties.getClients().get("default"));
            config.setEventLoopGroup(eventLoopGroup);
            config.setExecutor(executorService);
            config.setTransportMode(transportMode);
            repository.put(entry.getKey(), Redisson.create(config));
        }
    }

    public Optional<RedissonClient> getClient(String name) {
        return Optional.ofNullable(repository.get(name));
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
