package io.github.habatoo.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.math.BigDecimal;

/**
 * Конфигурация Redis для работы с балансами пользователей.
 * <p>
 * Настраивает реактивный шаблон {@link ReactiveRedisTemplate} с использованием
 * JSON-сериализации для значений типа {@link BigDecimal} и строковой сериализации для ключей.
 */
@Configuration
public class RedisConfiguration {

    /**
     * Создает реактивный шаблон для взаимодействия с Redis.
     *
     * @param factory      фабрика реактивных соединений с Redis.
     * @param objectMapper настроенный экземпляр Jackson для сериализации в JSON.
     * @return настроенный экземпляр {@link ReactiveRedisTemplate}.
     */
    @Bean
    public ReactiveRedisTemplate<String, BigDecimal> balanceRedisTemplate(
            ReactiveRedisConnectionFactory factory,
            ObjectMapper objectMapper) {

        Jackson2JsonRedisSerializer<BigDecimal> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, BigDecimal.class);
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisSerializationContext<String, BigDecimal> context = RedisSerializationContext.
                <String, BigDecimal>newSerializationContext(keySerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .key(keySerializer)
                .value(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
