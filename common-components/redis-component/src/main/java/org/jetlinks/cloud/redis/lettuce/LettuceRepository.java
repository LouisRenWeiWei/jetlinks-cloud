package org.jetlinks.cloud.redis.lettuce;

import io.lettuce.core.EpollProvider;
import io.lettuce.core.KqueueProvider;
import io.lettuce.core.codec.RedisCodec;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetlinks.cloud.redis.LettuceClientRepository;
import org.jetlinks.lettuce.LettucePlus;
import org.jetlinks.lettuce.codec.FstCodec;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public class LettuceRepository implements LettuceClientRepository {

    private Map<String, LettucePlus> repository = new HashMap<>();

    @Autowired
    private MultiLettuceProperties properties;


    @Autowired
    private ScheduledExecutorService executorService;

    @PreDestroy
    public void shutdown(){

    }
    @PostConstruct
    public void init() {
        int threadSize = properties.getThreadSize();

        Class<? extends EventExecutorGroup> groupClass = NioEventLoopGroup.class;

        if (EpollProvider.isAvailable()) {
            groupClass = EpollEventLoopGroup.class;
        } else if (KqueueProvider.isAvailable()) {
            groupClass = KQueueEventLoopGroup.class;
        }
//        EventExecutorGroup eventExecutors = DefaultEventLoopGroupProvider.createEventLoopGroup(groupClass, properties.getThreadSize());
//
//        DefaultClientResources resources = DefaultClientResources.builder()
//                .ioThreadPoolSize(threadSize)
//                .eventLoopGroupProvider(new DefaultEventLoopGroupProvider(threadSize))
//                .eventExecutorGroup(eventExecutors)
//                .computationThreadPoolSize(threadSize)
//                .build();

        RedisCodec<Object, Object> codec = new FstCodec<>();

        properties.getClients().forEach((key, value) -> repository.put(key, value.createLettucePlus(null, codec, executorService)));


    }

    @Override
    public Optional<LettucePlus> getClient(String name) {
        return Optional.ofNullable(repository.get(name));
    }
}
