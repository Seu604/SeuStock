package com.seu.seustock.service;

import com.seu.seustock.mapper.ItemMapper;
import com.seu.seustock.mapper.StockMapper;
import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.ItemDTO;
import com.seu.seustock.model.dto.StockDTO;
import com.seu.seustock.model.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    private static final String USERNAME = "testuser";
    private static final UUID ITEM_EXTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private ItemMapper itemMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void delete_rejectsItemWithStock() {
        ItemDTO item = new ItemDTO();
        item.setId(10L);
        item.setExternalId(ITEM_EXTERNAL_ID);
        item.setUserId(1L);
        UserDTO user = new UserDTO();
        user.setId(1L);

        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(userMapper.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(stockMapper.findByItemId(item.getId())).thenReturn(List.of(new StockDTO()));

        assertThatThrownBy(() -> itemService.delete(ITEM_EXTERNAL_ID, USERNAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("재고");

        verify(itemMapper, never()).deleteById(any());
    }

    @Test
    void delete_rejectsItemOwnedByAnotherUser() {
        ItemDTO item = new ItemDTO();
        item.setId(10L);
        item.setExternalId(ITEM_EXTERNAL_ID);
        item.setUserId(99L);
        UserDTO user = new UserDTO();
        user.setId(1L);

        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(userMapper.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> itemService.delete(ITEM_EXTERNAL_ID, USERNAME))
                .isInstanceOf(SecurityException.class);

        verify(itemMapper, never()).deleteById(any());
    }

    @Test
    void delete_rejectsNotFound() {
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.delete(ITEM_EXTERNAL_ID, USERNAME))
                .isInstanceOf(NoSuchElementException.class);

        verify(itemMapper, never()).deleteById(any());
    }

    @Test
    void delete_removesItemWithoutStock() {
        ItemDTO item = new ItemDTO();
        item.setId(10L);
        item.setExternalId(ITEM_EXTERNAL_ID);
        item.setUserId(1L);
        UserDTO user = new UserDTO();
        user.setId(1L);

        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(userMapper.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(stockMapper.findByItemId(item.getId())).thenReturn(List.of());

        itemService.delete(ITEM_EXTERNAL_ID, USERNAME);

        verify(itemMapper).deleteById(item.getId());
    }
}
