package org.jetlinks.cloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import java.util.concurrent.*;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Configuration
@EnableAsync
@Slf4j
public class ExecutorServiceConfiguration {

    @Bean
    public AsyncConfigurer asyncConfigurer() {
        AsyncUncaughtExceptionHandler handler = new SimpleAsyncUncaughtExceptionHandler();

        return new AsyncConfigurer() {
            @Override
            public Executor getAsyncExecutor() {
                return threadPoolTaskExecutor().getObject();
            }

            @Override
            public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                return handler;
            }
        };
    }

    @Bean
    @ConfigurationProperties(prefix = "jetlinks.executor")
    @Primary
    public ThreadPoolExecutorFactoryBean threadPoolTaskExecutor() {
        ThreadPoolExecutorFactoryBean executor = new ThreadPoolExecutorFactoryBean() {
            @Override
            protected ScheduledThreadPoolExecutor createExecutor(int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

                ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
                poolExecutor.setMaximumPoolSize(maxPoolSize);
                poolExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);
                poolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);

                return poolExecutor;
            }
        };
        executor.setThreadNamePrefix("jetlinks-thread-");
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        
        return executor;
    }

}
