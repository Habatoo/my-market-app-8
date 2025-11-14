package io.github.habatoo.mappers;

import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.entity.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Интеграционный тест ItemMapper — маппер")
class ItemMapperIntegrationTest {
    @Autowired
    private ItemMapper itemMapper;

    @Test
    @DisplayName("Маппинг Item в ItemDto с константным count")
    void mapSingleItemTest() {
        Item item = new Item();
        item.setId(11L);
        item.setTitle("Клавиатура");
        item.setDescription("Механическая");
        item.setPrice(BigDecimal.valueOf(150));

        ItemDto dto = itemMapper.toDto(item);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(11L);
        assertThat(dto.title()).isEqualTo("Клавиатура");
        assertThat(dto.description()).isEqualTo("Механическая");
        assertThat(dto.price()).isEqualTo(BigDecimal.valueOf(150));
        assertThat(dto.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Маппинг списка Item в список ItemDto")
    void mapItemListTest() {
        Item itemA = new Item();
        itemA.setId(1L);
        itemA.setTitle("A");
        itemA.setPrice(BigDecimal.valueOf(10));
        Item itemB = new Item();
        itemB.setId(2L);
        itemB.setTitle("B");
        itemB.setPrice(BigDecimal.valueOf(20));

        List<ItemDto> dtos = itemMapper.toDto(List.of(itemA, itemB));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).id()).isEqualTo(1L);
        assertThat(dtos.get(1).id()).isEqualTo(2L);
        assertThat(dtos.get(0).count()).isEqualTo(0);
        assertThat(dtos.get(1).count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Маппинг пустого списка Item")
    void mapEmptyItemListTest() {
        assertThat(itemMapper.toDto(List.of())).isEmpty();
    }
}
