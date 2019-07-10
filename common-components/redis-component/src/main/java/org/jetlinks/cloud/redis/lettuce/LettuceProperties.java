package org.jetlinks.cloud.redis.lettuce;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.resource.ClientResources;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetlinks.lettuce.LettucePlus;
import org.jetlinks.lettuce.codec.StringKeyCodec;
import org.jetlinks.lettuce.supports.DefaultLettucePlus;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;

@Getter
@Setter
public class LettuceProperties {

    private String[] hosts = {"redis://127.0.0.1:6379"};

    private String clientName;

    private String password;

    private int database = 0;

    private int poolSize = Runtime.getRuntime().availableProcessors() * 2;

    private int pubsubPoolSize = Runtime.getRuntime().availableProcessors();

    private String type = "standalone";

    private void init(RedisURI.Builder builder) {
        builder.withDatabase(database);
        if (null != password) {
            builder.withPassword(password);
        }
        if (null != clientName) {
            builder.withClientName(clientName);
        }
    }

    @SneakyThrows
    public LettucePlus createLettucePlus(ClientResources resources, RedisCodec<Object, Object> codec, ScheduledExecutorService executorService) {


        if ("standalone".equals(type)) {
            URI url = URI.create(hosts[0]);

            RedisURI.Builder redisURI = RedisURI.builder();
            redisURI.withHost(url.getHost());
            redisURI.withPort(url.getPort());
            init(redisURI);

            RedisClient client = RedisClient.create( redisURI.build());
            client.setOptions(ClientOptions.builder()
                    .autoReconnect(true)
                    .build());
            DefaultLettucePlus plus = new DefaultLettucePlus(client);
            plus.setExecutorService(executorService);
            plus.setPoolSize(poolSize);
            plus.setPubsubSize(pubsubPoolSize);
            plus.setDefaultCodec(new StringKeyCodec<>(codec));
            plus.init();
            plus.initStandalone();
            return plus;
        } else {
            RedisURI.Builder builder = RedisURI.builder();
            init(builder);

            RedisClient client = RedisClient.create( builder.build());
            client.setOptions(ClientOptions.builder()
                    .autoReconnect(true)
                    .build());

            RedisURI.Builder redisURI = RedisURI.builder();
            init(redisURI);
            for (String host : hosts) {
                URI url = URI.create(host);
                redisURI.withSentinel(url.getHost(), url.getPort());
                redisURI.withDatabase(database);
            }
            DefaultLettucePlus plus = new DefaultLettucePlus(client);
            plus.setExecutorService(executorService);
            plus.setPoolSize(poolSize);
            plus.setPubsubSize(pubsubPoolSize);
            plus.setDefaultCodec(new StringKeyCodec<>(codec));
            plus.init();
            plus.initSentinel(redisURI.build());
            return plus;
        }


    }
}
