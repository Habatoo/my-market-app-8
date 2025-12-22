package io.github.habatoo.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Тест реактивной конфигурации для Redis.
 */
@DisplayName("Юнит-тесты RedisConfiguration")
class RedisConfigurationTest {

    private final RedisConfiguration redisConfiguration = new RedisConfiguration();

    /**
     * Тестирование сериализации ключей с использованием стандартного сериализатора.
     */
    @Test
    @DisplayName("Проверка сериализации BigDecimal через настроенный Template")
    void shouldCorrectlySerializeBigDecimalTest() {
        ReactiveRedisConnectionFactory factory = mock(ReactiveRedisConnectionFactory.class);
        ObjectMapper objectMapper = new ObjectMapper();
        BigDecimal testValue = new BigDecimal("123.45");

        ReactiveRedisTemplate<String, BigDecimal> template =
                redisConfiguration.balanceRedisTemplate(factory, objectMapper);
        RedisSerializationContext<String, BigDecimal> context = template.getSerializationContext();

        ByteBuffer serialized = context.getValueSerializationPair()
                .getWriter()
                .write(testValue);

        String jsonResult = StandardCharsets.UTF_8.decode(serialized).toString();

        ByteBuffer buffer = ByteBuffer.wrap(jsonResult.getBytes(StandardCharsets.UTF_8));
        BigDecimal deserialized = context.getValueSerializationPair()
                .getReader()
                .read(buffer);

        assertEquals("123.45", jsonResult, "Значение должно быть сериализовано в JSON формат");
        assertEquals(testValue, deserialized, "Десериализованное значение должно совпадать с исходным");
    }

    /**
     * Тестирование сериализации ключей с использованием стандартного сериализатора.
     */
    @Test
    @DisplayName("Проверка сериализации ключей (String)")
    void shouldSerializeKeysAsStringTest() {
        ReactiveRedisConnectionFactory factory = mock(ReactiveRedisConnectionFactory.class);
        ReactiveRedisTemplate<String, BigDecimal> template =
                redisConfiguration.balanceRedisTemplate(factory, new ObjectMapper());

        ByteBuffer serializedKey = template.getSerializationContext()
                .getKeySerializationPair()
                .getWriter()
                .write("user:123");

        String resultKey = StandardCharsets.UTF_8.decode(serializedKey).toString();
        assertEquals("user:123", resultKey);
    }
}
