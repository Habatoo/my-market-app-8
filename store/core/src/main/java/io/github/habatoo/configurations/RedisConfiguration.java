package io.github.habatoo.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.habatoo.dto.response.ItemDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
@EnableRedisRepositories(basePackages = "io.github.habatoo.storages")
public class RedisConfiguration {

    @Bean
    public ReactiveRedisTemplate<String, ItemDto> itemRedisTemplate(
            ReactiveRedisConnectionFactory factory,
            ObjectMapper objectMapper) {

        Jackson2JsonRedisSerializer<ItemDto> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, ItemDto.class);
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisSerializationContext<String, ItemDto> context = RedisSerializationContext.
                <String, ItemDto>newSerializationContext(keySerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .key(keySerializer)
                .value(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, List<ItemDto>> itemsListRedisTemplate(
            ReactiveRedisConnectionFactory factory,
            ObjectMapper objectMapper) {

        Jackson2JsonRedisSerializer<List<ItemDto>> valueSerializer =
                new Jackson2JsonRedisSerializer<>(
                        objectMapper,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ItemDto.class));

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisSerializationContext<String, List<ItemDto>> context = RedisSerializationContext
                .<String, List<ItemDto>>newSerializationContext(keySerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .key(keySerializer)
                .value(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
