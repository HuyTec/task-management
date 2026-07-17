package com.taskmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskmanagement.dto.expense.ExpenseResponse;
import com.taskmanagement.dto.task.TaskDetailResponse;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, TaskDetailResponse> taskRedisTemplate(RedisConnectionFactory connectionFactory) {
        return buildTemplate(connectionFactory, TaskDetailResponse.class);
    }

    @Bean
    public RedisTemplate<String, ExpenseResponse> expenseRedisTemplate(RedisConnectionFactory connectionFactory) {
        return buildTemplate(connectionFactory, ExpenseResponse.class);
    }

    private <T> RedisTemplate<String, T> buildTemplate(RedisConnectionFactory connectionFactory, Class<T> clazz) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<T> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, clazz);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);

        return template;
    }
}