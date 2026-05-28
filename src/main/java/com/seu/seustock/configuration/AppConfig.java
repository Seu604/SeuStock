package com.seu.seustock.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AppConfig {

    @Value("${security.bcrypt.strength:10}")
    private int bcryptStrength;

    @Value("${seustock.ai.executor.core-pool-size:2}")
    private int aiExecutorCorePoolSize;

    @Value("${seustock.ai.executor.max-pool-size:4}")
    private int aiExecutorMaxPoolSize;

    @Value("${seustock.ai.executor.queue-capacity:10}")
    private int aiExecutorQueueCapacity;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }

    // Java 직렬화 대신 JSON 직렬화로 배포 간 세션 호환성 확보
    // GenericJackson2JsonRedisSerializer 대신 default typing ObjectMapper 사용 (Spring Data Redis 4.x 권장)
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return new Jackson2JsonRedisSerializer<>(mapper, Object.class);
    }

    @Bean(name = "aiAnalysisExecutor")
    public Executor aiAnalysisExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(aiExecutorCorePoolSize);
        exec.setMaxPoolSize(aiExecutorMaxPoolSize);
        exec.setQueueCapacity(aiExecutorQueueCapacity);
        exec.setThreadNamePrefix("ai-analysis-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        exec.initialize();
        return exec;
    }

    @Bean
    public LocalValidatorFactoryBean getValidator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
