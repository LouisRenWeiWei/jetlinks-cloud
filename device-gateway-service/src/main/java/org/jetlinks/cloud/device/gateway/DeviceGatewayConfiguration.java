package org.jetlinks.cloud.device.gateway;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.mqtt.MqttServerOptions;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.cloud.device.gateway.events.DeviceOfflineEvent;
import org.jetlinks.cloud.device.gateway.events.DeviceOnlineEvent;
import org.jetlinks.cloud.device.gateway.vertx.VerticleSupplier;
import org.jetlinks.cloud.redis.RedissonClientRepository;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.registry.DeviceMessageHandler;
import org.jetlinks.core.device.registry.DeviceRegistry;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.gateway.monitor.GatewayServerMonitor;
import org.jetlinks.gateway.monitor.RedissonGatewayServerMonitor;
import org.jetlinks.gateway.session.DefaultDeviceSessionManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Configuration
public class DeviceGatewayConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "vertx")
    public VertxOptions vertxOptions() {
        return new VertxOptions();
    }

    @Bean
    @ConfigurationProperties(prefix = "vertx.mqtt")
    public MqttServerOptions mqttServerOptions() {
        return new MqttServerOptions();
    }

    @Bean
    public Vertx vertx(VertxOptions vertxOptions) {
        return Vertx.vertx(vertxOptions);
    }


    @Bean
    public RedissonGatewayServerMonitor deviceMonitor(Environment environment,
                                                      ScheduledExecutorService executorService,
                                                      RedissonClientRepository repository) {
        return new RedissonGatewayServerMonitor(environment.getProperty("gateway.server-id"),
                repository.getClient("device-registry")
                        .orElseGet(repository::getDefaultClient), executorService);
    }

    @Bean(initMethod = "init", destroyMethod = "shutdown")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public DefaultDeviceSessionManager deviceSessionManager(Environment environment,
                                                            ProtocolSupports protocolSupports,
                                                            DeviceRegistry registry,
                                                            DeviceMessageHandler deviceMessageHandler,
                                                            GatewayServerMonitor gatewayServerMonitor,
                                                            ApplicationEventPublisher eventPublisher,
                                                            DeviceSessionManagerProperties sessionManagerProperties,
                                                            ScheduledExecutorService executorService) {
        DefaultDeviceSessionManager sessionManager = new DefaultDeviceSessionManager();
        sessionManager.setServerId(environment.getProperty("gateway.server-id"));
        sessionManager.setProtocolSupports(protocolSupports);
        sessionManager.setDeviceRegistry(registry);
        sessionManager.setExecutorService(executorService);
        sessionManager.setGatewayServerMonitor(gatewayServerMonitor);
        sessionManager.setDeviceMessageHandler(deviceMessageHandler);
        sessionManager.setOnDeviceRegister(session -> eventPublisher.publishEvent(new DeviceOnlineEvent(session, System.currentTimeMillis())));
        sessionManager.setOnDeviceUnRegister(session -> eventPublisher.publishEvent(new DeviceOfflineEvent(session, System.currentTimeMillis() + 100)));

        sessionManager.setTransportLimits(sessionManagerProperties.getConnectionLimits());

        return sessionManager;
    }

    @ConfigurationProperties(prefix = "device.session")
    @Component
    @Getter
    @Setter
    public static class DeviceSessionManagerProperties {

        public DeviceSessionManagerProperties() {
            long maxConnection = (Runtime.getRuntime().maxMemory() / 1024 / 1024) * 90;

            connectionLimits.put(Transport.MQTT, maxConnection);
        }

        private Map<Transport, Long> connectionLimits = new ConcurrentHashMap<>();

    }

    @Bean
    public VertxServerInitializer mqttServerInitializer() {
        return new VertxServerInitializer();
    }

    @Slf4j
    public static class VertxServerInitializer implements CommandLineRunner, DisposableBean {

        @Autowired
        private VerticleFactory verticleFactory;

        @Autowired
        private List<VerticleSupplier> verticles;

        @Autowired
        private Vertx vertx;

        @Override
        public void run(String... args) {
            vertx.registerVerticleFactory(verticleFactory);
            for (VerticleSupplier supplier : verticles) {
                DeploymentOptions options = new DeploymentOptions();
                options.setHa(true);
                options.setInstances(supplier.getInstances());
                vertx.deployVerticle(supplier, options, e -> {
                    if (!e.succeeded()) {
                        log.error("deploy verticle :{} error", supplier, e.cause());
                    } else {
                        log.debug("deploy verticle :{} success", supplier);
                    }
                });
            }
        }

        @Override
        public void destroy() throws Exception {
            log.debug("close vertx");
            CountDownLatch latch = new CountDownLatch(1);
            vertx.close(result -> {
                log.debug("close vertx done");
                latch.countDown();
            });
            latch.await(30, TimeUnit.SECONDS);
        }
    }
}
