package org.jetlinks.cloud.redis.lettuce;

import org.hswebframework.web.authorization.token.UserTokenManager;
import org.jetlinks.cloud.redis.AutoClearCache;
import org.jetlinks.cloud.redis.LettuceClientRepository;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.registry.DeviceMessageHandler;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.lettuce.spring.cache.LettuceCacheManager;
import org.jetlinks.lettuce.spring.hsweb.LettuceUserTokenManager;
import org.jetlinks.registry.redis.lettuce.LettuceDeviceMessageHandler;
import org.jetlinks.registry.redis.lettuce.LettuceDeviceRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(MultiLettuceProperties.class)
@SuppressWarnings("all")
public class LettuceConfiguration {

    @Bean
    public LettuceClientRepository lettuceClientRepository() {
        return new LettuceRepository();
    }

    @Bean
    public CacheManager cacheManager(LettuceClientRepository lettuceClientRepository) {

        CacheManager cacheManager=new LettuceCacheManager(lettuceClientRepository.getDefaultClient());

        return new TransactionAwareCacheManagerProxy(cacheManager) {
            @Override
            public Cache getCache(String name) {
                return new AutoClearCache(super.getCache(name));
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "jetlinks.redis.user-token", name = "enable", havingValue = "true")
    @ConfigurationProperties(prefix = "jetlinks.authorize")
    public UserTokenManager userTokenManager(LettuceClientRepository lettuceClientRepository) {

        return new LettuceUserTokenManager("jetlinks",lettuceClientRepository.getClient("user-token").orElseGet(lettuceClientRepository::getDefaultClient));
    }


    @Bean
    public LettuceDeviceMessageHandler deviceMessageHandler(LettuceClientRepository lettuceClientRepository) {
        return new LettuceDeviceMessageHandler(lettuceClientRepository.getClient("device-registry")
                .orElseGet(lettuceClientRepository::getDefaultClient));
    }

    @Bean
    public LettuceDeviceRegistry deviceRegistry(LettuceClientRepository lettuceClientRepository,
                                                DeviceMessageHandler messageHandler,
                                                ProtocolSupports protocolSupports) {

        return new LettuceDeviceRegistry(
                lettuceClientRepository.getClient("device-registry").orElseGet(lettuceClientRepository::getDefaultClient),
                messageHandler,
                protocolSupports);
    }

    @Bean
    public BeanPostProcessor deviceMessageSenderAutoRegister(LettuceDeviceRegistry registry) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
                return o;
            }

            @Override
            public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
                if (o instanceof DeviceMessageSenderInterceptor) {
                    registry.addInterceptor(((DeviceMessageSenderInterceptor) o));
                }
                return o;
            }
        };
    }

}
