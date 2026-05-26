package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.ItemDTO;
import com.seu.seustock.model.dto.SpaceDTO;
import com.seu.seustock.model.dto.StockDTO;
import com.seu.seustock.model.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("classpath:schema-test.sql")
class ItemMapperTest {

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SpaceMapper spaceMapper;

    @Autowired
    private StockMapper stockMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        UserDTO user = new UserDTO();
        user.setUsername("testuser");
        user.setPassword("password");
        userMapper.insertUser(user);
        userId = user.getId();
    }

    private ItemDTO buildItem(String name, String description) {
        ItemDTO item = new ItemDTO();
        item.setUserId(userId);
        item.setName(name);
        item.setDescription(description);
        return item;
    }

    @Test
    void insertItem_thenFindById() {
        ItemDTO item = buildItem("노트북", "업무용 노트북");
        itemMapper.insertItem(item);

        Optional<ItemDTO> found = itemMapper.findById(item.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getExternalId()).isNotNull();
        assertThat(found.get().getName()).isEqualTo("노트북");
        assertThat(found.get().getDescription()).isEqualTo("업무용 노트북");
        assertThat(found.get().getUserId()).isEqualTo(userId);
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void findById_notFound_returnsEmpty() {
        Optional<ItemDTO> found = itemMapper.findById(999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findByExternalId_includesStockAndSpaceCounts() {
        ItemDTO item = buildItem("노트북", null);
        itemMapper.insertItem(item);
        SpaceDTO space = new SpaceDTO();
        space.setUserId(userId);
        space.setName("창고");
        spaceMapper.insertSpace(space);
        StockDTO stock = new StockDTO();
        stock.setItemId(item.getId());
        stock.setSpaceId(space.getId());
        stockMapper.insertStock(stock);
        ItemDTO persisted = itemMapper.findById(item.getId()).orElseThrow();

        Optional<ItemDTO> found = itemMapper.findByExternalId(persisted.getExternalId());

        assertThat(found).isPresent();
        assertThat(found.get().getStockCount()).isEqualTo(1);
        assertThat(found.get().getSpaceCount()).isEqualTo(1);
    }

    @Test
    void findByUserId() {
        itemMapper.insertItem(buildItem("노트북", null));
        itemMapper.insertItem(buildItem("마우스", null));

        List<ItemDTO> items = itemMapper.findByUserId(userId);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(ItemDTO::getName).containsExactlyInAnyOrder("노트북", "마우스");
    }

    @Test
    void findByUserIdWithOptions_filtersByNameAndSorts() {
        itemMapper.insertItem(buildItem("노트북", null));
        itemMapper.insertItem(buildItem("무선 마우스", null));
        itemMapper.insertItem(buildItem("유선 마우스", null));

        List<ItemDTO> searched = itemMapper.findByUserIdWithOptions(userId, "마우스", "name", 10, 0);
        List<ItemDTO> oldest = itemMapper.findByUserIdWithOptions(userId, null, "oldest", 10, 0);

        assertThat(searched).extracting(ItemDTO::getName).containsExactly("무선 마우스", "유선 마우스");
        assertThat(oldest).extracting(ItemDTO::getName).containsExactly("노트북", "무선 마우스", "유선 마우스");
        assertThat(itemMapper.countByUserIdWithOptions(userId, "마우스")).isEqualTo(2);
    }

    @Test
    void findByUserIdWithOptions_appliesLimitAndOffset() {
        for (int i = 0; i < 12; i++) {
            itemMapper.insertItem(buildItem("품목%02d".formatted(i), null));
        }

        List<ItemDTO> firstPage = itemMapper.findByUserIdWithOptions(userId, null, "name", 10, 0);
        List<ItemDTO> secondPage = itemMapper.findByUserIdWithOptions(userId, null, "name", 10, 10);

        assertThat(firstPage).hasSize(10);
        assertThat(secondPage).extracting(ItemDTO::getName).containsExactly("품목10", "품목11");
        assertThat(itemMapper.countByUserIdWithOptions(userId, null)).isEqualTo(12);
    }

    @Test
    void deactivateById_excludesItemFromUserList() {
        ItemDTO activeItem = buildItem("활성아이템", null);
        itemMapper.insertItem(activeItem);
        ItemDTO inactiveItem = buildItem("비활성아이템", null);
        itemMapper.insertItem(inactiveItem);

        itemMapper.deactivateById(inactiveItem.getId());

        Optional<ItemDTO> found = itemMapper.findById(inactiveItem.getId());
        List<ItemDTO> items = itemMapper.findByUserId(userId);

        assertThat(found).isPresent();
        assertThat(found.get().isActive()).isFalse();
        assertThat(items).extracting(ItemDTO::getName).containsExactly("활성아이템");
    }

    @Test
    void updateItem() {
        ItemDTO item = buildItem("구형노트북", "오래된 노트북");
        itemMapper.insertItem(item);
        item.setName("신형노트북");
        item.setDescription("최신 노트북");

        itemMapper.updateItem(item);

        Optional<ItemDTO> found = itemMapper.findById(item.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("신형노트북");
        assertThat(found.get().getDescription()).isEqualTo("최신 노트북");
    }

    @Test
    void deleteById() {
        ItemDTO item = buildItem("삭제아이템", null);
        itemMapper.insertItem(item);

        itemMapper.deleteById(item.getId());

        assertThat(itemMapper.findById(item.getId())).isEmpty();
    }
}
