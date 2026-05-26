package com.seu.seustock.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AppConfig {

    @Value("${security.bcrypt.strength:10}")
    private int bcryptStrength;

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
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(4);
        exec.setQueueCapacity(10);
        exec.setThreadNamePrefix("ai-analysis-");
        exec.initialize();
        return exec;
    }
}
