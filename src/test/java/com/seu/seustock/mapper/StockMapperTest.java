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
class StockMapperTest {

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private SpaceMapper spaceMapper;

    @Autowired
    private UserMapper userMapper;

    private Long itemId;
    private Long spaceId;

    @BeforeEach
    void setUp() {
        UserDTO user = new UserDTO();
        user.setUsername("testuser");
        user.setPassword("password");
        userMapper.insertUser(user);

        ItemDTO item = new ItemDTO();
        item.setUserId(user.getId());
        item.setName("노트북");
        itemMapper.insertItem(item);
        itemId = item.getId();

        SpaceDTO space = new SpaceDTO();
        space.setUserId(user.getId());
        space.setName("창고");
        spaceMapper.insertSpace(space);
        spaceId = space.getId();
    }

    private StockDTO buildStock(int quantity) {
        StockDTO stock = new StockDTO();
        stock.setItemId(itemId);
        stock.setSpaceId(spaceId);
        stock.setQuantity(quantity);
        return stock;
    }

    @Test
    void insertStock_thenFindById() {
        StockDTO stock = buildStock(10);
        stockMapper.insertStock(stock);

        Optional<StockDTO> found = stockMapper.findById(stock.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getExternalId()).isNotNull();
        assertThat(found.get().getItemId()).isEqualTo(itemId);
        assertThat(found.get().getSpaceId()).isEqualTo(spaceId);
        assertThat(found.get().getQuantity()).isEqualTo(10);
    }

    @Test
    void findById_notFound_returnsEmpty() {
        Optional<StockDTO> found = stockMapper.findById(999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findByItemId() {
        stockMapper.insertStock(buildStock(5));
        stockMapper.insertStock(buildStock(3));

        List<StockDTO> stocks = stockMapper.findByItemId(itemId);

        assertThat(stocks).hasSize(2);
    }

    @Test
    void findBySpaceId() {
        stockMapper.insertStock(buildStock(7));

        List<StockDTO> stocks = stockMapper.findBySpaceId(spaceId);

        assertThat(stocks).hasSize(1);
        assertThat(stocks.get(0).getQuantity()).isEqualTo(7);
    }

    @Test
    void updateStock() {
        StockDTO stock = buildStock(5);
        stockMapper.insertStock(stock);
        stock.setQuantity(20);

        stockMapper.updateStock(stock);

        Optional<StockDTO> found = stockMapper.findById(stock.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(20);
    }

    @Test
    void deleteById() {
        StockDTO stock = buildStock(1);
        stockMapper.insertStock(stock);

        stockMapper.deleteById(stock.getId());

        assertThat(stockMapper.findById(stock.getId())).isEmpty();
    }
}
