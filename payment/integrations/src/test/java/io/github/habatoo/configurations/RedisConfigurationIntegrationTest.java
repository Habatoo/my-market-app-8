package io.github.habatoo.configurations;

import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Интеграционный тест конфигурации Redis")
class RedisConfigurationIntegrationTest extends BaseTest {

    @Autowired
    private ReactiveRedisTemplate<String, BigDecimal> balanceRedisTemplate;

    @Test
    @DisplayName("Бин balanceRedisTemplate должен быть успешно загружен")
    void templateBeanShouldBeLoadedTest() {
        assertThat(balanceRedisTemplate).isNotNull();
    }

    @Test
    @DisplayName("Сериализация BigDecimal в JSON должна работать корректно")
    void serializationAndDeserializationTest() {
        String testKey = "test:config:balance";
        BigDecimal originalBalance = new BigDecimal("1234.5678");

        StepVerifier.create(
                        balanceRedisTemplate.opsForValue().set(testKey, originalBalance)
                                .then(balanceRedisTemplate.opsForValue().get(testKey))
                )
                .assertNext(retrievedBalance -> {
                    assertThat(retrievedBalance).isNotNull();
                    assertThat(retrievedBalance).isEqualByComparingTo(originalBalance);
                })
                .verifyComplete();

        balanceRedisTemplate.opsForValue().delete(testKey).subscribe();
    }

    @Test
    @DisplayName("Сериализация должна сохранять точность больших чисел")
    void highPrecisionSerializationTest() {
        String testKey = "test:config:precision";
        BigDecimal bigValue = new BigDecimal("999999999999.99");

        StepVerifier.create(
                        balanceRedisTemplate.opsForValue().set(testKey, bigValue)
                                .then(balanceRedisTemplate.opsForValue().get(testKey))
                )
                .assertNext(val -> assertThat(val).isEqualByComparingTo(bigValue))
                .verifyComplete();
    }
}
