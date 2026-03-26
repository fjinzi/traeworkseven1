package com.seckill.config;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.database}")
    private int redisDatabase;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(redisPort);
            config.setDatabase(redisDatabase);

            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(3))
                    .shutdownTimeout(Duration.ofSeconds(1))
                    .build();

            LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
            factory.afterPropertiesSet();
            
            // Test connection
            factory.getConnection().close();
            log.info("Redis连接成功: {}:{}", redisHost, redisPort);
            return factory;
        } catch (Exception e) {
            log.warn("Redis连接失败，将禁用Redis功能: {}:{}，错误: {}", redisHost, redisPort, e.getMessage());
            return null;
        }
    }

    @Bean
    public RedissonClient redissonClient() {
        try {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://" + redisHost + ":" + redisPort)
                    .setPassword(null)
                    .setDatabase(redisDatabase)
                    .setConnectionMinimumIdleSize(2)
                    .setConnectionPoolSize(10)
                    .setConnectTimeout(5000)
                    .setTimeout(3000)
                    .setRetryAttempts(1)
                    .setRetryInterval(1000);
            return Redisson.create(config);
        } catch (Exception e) {
            log.warn("Redis连接失败，RedissonClient未创建: {}", e.getMessage());
            return null;
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        try {
            RedisConnectionFactory connectionFactory = redisConnectionFactory();
            if (connectionFactory == null) {
                log.warn("RedisConnectionFactory不可用，返回空RedisTemplate");
                RedisTemplate<String, Object> emptyTemplate = new RedisTemplate<>();
                emptyTemplate.setEnableTransactionSupport(false);
                return emptyTemplate;
            }
            
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            objectMapper.activateDefaultTyping(
                    LaissezFaireSubTypeValidator.instance,
                    ObjectMapper.DefaultTyping.NON_FINAL,
                    JsonTypeInfo.As.PROPERTY
            );

            GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(serializer);
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(serializer);
            template.setEnableTransactionSupport(false);
            template.afterPropertiesSet();

            return template;
        } catch (Exception e) {
            log.warn("RedisTemplate创建失败，返回空实例: {}", e.getMessage());
            RedisTemplate<String, Object> emptyTemplate = new RedisTemplate<>();
            emptyTemplate.setEnableTransactionSupport(false);
            return emptyTemplate;
        }
    }

}