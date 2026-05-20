package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.ItemDTO;
import com.seu.seustock.model.dto.SpaceDTO;
import com.seu.seustock.model.dto.StockDTO;
import com.seu.seustock.model.dto.StockTransactionDTO;
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
class StockTransactionMapperTest {

    @Autowired
    private StockTransactionMapper stockTransactionMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private SpaceMapper spaceMapper;

    @Autowired
    private UserMapper userMapper;

    private Long stockId;

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

        SpaceDTO space = new SpaceDTO();
        space.setUserId(user.getId());
        space.setName("창고");
        spaceMapper.insertSpace(space);

        StockDTO stock = new StockDTO();
        stock.setItemId(item.getId());
        stock.setSpaceId(space.getId());
        stockMapper.insertStock(stock);
        stockId = stock.getId();
    }

    private StockTransactionDTO buildTransaction(String type, String memo) {
        StockTransactionDTO tx = new StockTransactionDTO();
        tx.setStockId(stockId);
        tx.setTransactionType(type);
        tx.setMemo(memo);
        return tx;
    }

    @Test
    void insertTransaction_thenFindById() {
        StockTransactionDTO tx = buildTransaction("IN", "입고");
        stockTransactionMapper.insertTransaction(tx);

        Optional<StockTransactionDTO> found = stockTransactionMapper.findById(tx.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getExternalId()).isNotNull();
        assertThat(found.get().getStockId()).isEqualTo(stockId);
        assertThat(found.get().getTransactionType()).isEqualTo("IN");
        assertThat(found.get().getMemo()).isEqualTo("입고");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        Optional<StockTransactionDTO> found = stockTransactionMapper.findById(999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findByStockId_returnsAllTransactions() {
        stockTransactionMapper.insertTransaction(buildTransaction("IN", "입고"));
        stockTransactionMapper.insertTransaction(buildTransaction("OUT", "출고"));

        List<StockTransactionDTO> txList = stockTransactionMapper.findByStockId(stockId);

        assertThat(txList).hasSize(2);
        assertThat(txList).extracting(StockTransactionDTO::getTransactionType)
                .containsExactly("IN", "OUT");
    }

    @Test
    void findByStockId_noTransactions_returnsEmpty() {
        List<StockTransactionDTO> txList = stockTransactionMapper.findByStockId(stockId);
        assertThat(txList).isEmpty();
    }
}
