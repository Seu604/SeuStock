package com.seu.seustock.service;

import com.seu.seustock.mapper.SpaceMapper;
import com.seu.seustock.mapper.StockMapper;
import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.SpaceDTO;
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
class SpaceServiceTest {

    private static final String USERNAME = "testuser";
    private static final UUID SPACE_EXTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Mock
    private SpaceMapper spaceMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private SpaceService spaceService;

    @Test
    void delete_rejectsSpaceWithStock() {
        SpaceDTO space = new SpaceDTO();
        space.setId(10L);
        space.setExternalId(SPACE_EXTERNAL_ID);
        space.setUserId(1L);
        UserDTO user = new UserDTO();
        user.setId(1L);

        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(userMapper.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(stockMapper.findBySpaceId(space.getId())).thenReturn(List.of(new StockDTO()));

        assertThatThrownBy(() -> spaceService.delete(SPACE_EXTERNAL_ID, USERNAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("재고");

        verify(spaceMapper, never()).deleteById(any());
    }

    @Test
    void delete_rejectsSpaceOwnedByAnotherUser() {
        SpaceDTO space = new SpaceDTO();
        space.setId(10L);
        space.setExternalId(SPACE_EXTERNAL_ID);
        space.setUserId(99L);
        UserDTO user = new UserDTO();
        user.setId(1L);

        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(userMapper.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> spaceService.delete(SPACE_EXTERNAL_ID, USERNAME))
                .isInstanceOf(SecurityException.class);

        verify(spaceMapper, never()).deleteById(any());
    }

    @Test
    void delete_rejectsNotFound() {
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spaceService.delete(SPACE_EXTERNAL_ID, USERNAME))
                .isInstanceOf(NoSuchElementException.class);

        verify(spaceMapper, never()).deleteById(any());
    }

    @Test
    void delete_removesSpaceWithoutStock() {
        SpaceDTO space = new SpaceDTO();
        space.setId(10L);
        space.setExternalId(SPACE_EXTERNAL_ID);
        space.setUserId(1L);
        UserDTO user = new UserDTO();
        user.setId(1L);

        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(userMapper.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(stockMapper.findBySpaceId(space.getId())).thenReturn(List.of());

        spaceService.delete(SPACE_EXTERNAL_ID, USERNAME);

        verify(spaceMapper).deleteById(space.getId());
    }
}
