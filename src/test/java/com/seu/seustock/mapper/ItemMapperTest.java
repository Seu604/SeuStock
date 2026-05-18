package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.ItemDTO;
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
    }

    @Test
    void findById_notFound_returnsEmpty() {
        Optional<ItemDTO> found = itemMapper.findById(999L);
        assertThat(found).isEmpty();
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
