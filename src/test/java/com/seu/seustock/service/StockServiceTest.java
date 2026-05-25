package com.seu.seustock.service;

import com.seu.seustock.mapper.*;
import com.seu.seustock.model.enumeration.StockStatus;
import com.seu.seustock.model.enumeration.TransactionType;
import com.seu.seustock.model.dto.StockTransactionDTO;
import com.seu.seustock.model.dto.*;
import com.seu.seustock.model.form.StockForm;
import com.seu.seustock.model.form.StockInOutForm;
import com.seu.seustock.model.form.StockMoveForm;
import com.seu.seustock.model.form.StockUpdateForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    private static final String USERNAME = "testuser";
    private static final UUID ITEM_EXTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ITEM_EXTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SPACE_EXTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OTHER_SPACE_EXTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SHELF_EXTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");
    private static final UUID OTHER_SHELF_EXTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID BOX_EXTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000001000");
    private static final UUID OTHER_BOX_EXTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000001001");
    private static final UUID STOCK_EXTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000010000");

    @Mock
    private StockMapper stockMapper;
    @Mock
    private StockTransactionMapper transactionMapper;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private SpaceMapper spaceMapper;
    @Mock
    private ShelfMapper shelfMapper;
    @Mock
    private BoxMapper boxMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private StockService stockService;

    private UserDTO user;
    private ItemDTO item;
    private ItemDTO otherUserItem;
    private SpaceDTO space;
    private SpaceDTO otherSpace;
    private ShelfDTO shelf;
    private ShelfDTO otherSpaceShelf;
    private BoxDTO box;
    private BoxDTO otherShelfBox;

    @BeforeEach
    void setUp() {
        user = user(1L);
        item = item(10L, ITEM_EXTERNAL_ID, user.getId());
        otherUserItem = item(20L, OTHER_ITEM_EXTERNAL_ID, 2L);
        space = space(100L, SPACE_EXTERNAL_ID, user.getId());
        otherSpace = space(200L, OTHER_SPACE_EXTERNAL_ID, user.getId());
        shelf = shelf(1000L, SHELF_EXTERNAL_ID, space.getId());
        otherSpaceShelf = shelf(1001L, OTHER_SHELF_EXTERNAL_ID, otherSpace.getId());
        box = box(10000L, BOX_EXTERNAL_ID, shelf.getId());
        otherShelfBox = box(10001L, OTHER_BOX_EXTERNAL_ID, otherSpaceShelf.getId());

        when(userMapper.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    }

    @Test
    void create_rejectsItemOwnedByAnotherUser() {
        when(itemMapper.findByExternalId(OTHER_ITEM_EXTERNAL_ID)).thenReturn(Optional.of(otherUserItem));

        assertThatThrownBy(() -> stockService.create(stockForm(OTHER_ITEM_EXTERNAL_ID, SPACE_EXTERNAL_ID, null, null), USERNAME))
                .isInstanceOf(SecurityException.class);

        verify(stockMapper, never()).insertStock(any());
    }

    @Test
    void create_rejectsInactiveItem() {
        item.setActive(false);
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> stockService.create(stockForm(ITEM_EXTERNAL_ID, SPACE_EXTERNAL_ID, null, null), USERNAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("비활성화");

        verify(stockMapper, never()).insertStock(any());
    }

    @Test
    void create_rejectsShelfFromDifferentSpace() {
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(shelfMapper.findByExternalId(OTHER_SHELF_EXTERNAL_ID)).thenReturn(Optional.of(otherSpaceShelf));

        assertThatThrownBy(() -> stockService.create(stockForm(ITEM_EXTERNAL_ID, SPACE_EXTERNAL_ID, OTHER_SHELF_EXTERNAL_ID, null), USERNAME))
                .isInstanceOf(SecurityException.class);

        verify(stockMapper, never()).insertStock(any());
    }

    @Test
    void create_rejectsBoxWithoutShelf() {
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));

        assertThatThrownBy(() -> stockService.create(stockForm(ITEM_EXTERNAL_ID, SPACE_EXTERNAL_ID, null, BOX_EXTERNAL_ID), USERNAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("선반 정보");

        verify(stockMapper, never()).insertStock(any());
    }

    @Test
    void create_rejectsBoxFromDifferentShelf() {
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(shelfMapper.findByExternalId(SHELF_EXTERNAL_ID)).thenReturn(Optional.of(shelf));
        when(boxMapper.findByExternalId(OTHER_BOX_EXTERNAL_ID)).thenReturn(Optional.of(otherShelfBox));

        assertThatThrownBy(() -> stockService.create(stockForm(ITEM_EXTERNAL_ID, SPACE_EXTERNAL_ID, SHELF_EXTERNAL_ID, OTHER_BOX_EXTERNAL_ID), USERNAME))
                .isInstanceOf(SecurityException.class);

        verify(stockMapper, never()).insertStock(any());
    }

    @Test
    void create_usesVerifiedLocationIds() {
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(shelfMapper.findByExternalId(SHELF_EXTERNAL_ID)).thenReturn(Optional.of(shelf));
        when(boxMapper.findByExternalId(BOX_EXTERNAL_ID)).thenReturn(Optional.of(box));
        doAnswer(invocation -> {
            StockDTO stock = invocation.getArgument(0);
            stock.setId(500L);
            return null;
        }).when(stockMapper).insertStock(any());

        stockService.create(stockForm(ITEM_EXTERNAL_ID, SPACE_EXTERNAL_ID, SHELF_EXTERNAL_ID, BOX_EXTERNAL_ID), USERNAME);

        ArgumentCaptor<StockDTO> stockCaptor = ArgumentCaptor.forClass(StockDTO.class);
        verify(stockMapper).insertStock(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getItemId()).isEqualTo(item.getId());
        assertThat(stockCaptor.getValue().getSpaceId()).isEqualTo(space.getId());
        assertThat(stockCaptor.getValue().getShelfId()).isEqualTo(shelf.getId());
        assertThat(stockCaptor.getValue().getBoxId()).isEqualTo(box.getId());
        verify(transactionMapper).insertTransaction(any());
    }

    @Test
    void dispatchUnits_rejectsChangedStockState() {
        StockInOutForm form = stockInOutForm(SPACE_EXTERNAL_ID, null, null);
        StockDTO stock = new StockDTO();
        stock.setId(700L);
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(stockMapper.findInStockByItemAndSpace(item.getId(), space.getId())).thenReturn(List.of(stock));
        when(stockMapper.updateStatusIfInStock(stock.getId(), StockStatus.DISPATCHED)).thenReturn(0);

        assertThatThrownBy(() -> stockService.dispatchUnits(form, USERNAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("상태");

        verify(transactionMapper, never()).insertTransaction(any());
    }

    @Test
    void deleteUnits_rejectsManipulatedLocation() {
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(shelfMapper.findByExternalId(OTHER_SHELF_EXTERNAL_ID)).thenReturn(Optional.of(otherSpaceShelf));

        assertThatThrownBy(() -> stockService.deleteUnits(ITEM_EXTERNAL_ID, SPACE_EXTERNAL_ID, OTHER_SHELF_EXTERNAL_ID, null, USERNAME))
                .isInstanceOf(SecurityException.class);

        verify(stockMapper, never()).deleteInStockByItemAndShelf(any(), any());
    }

    @Test
    void deleteUnit_deletesOwnedInStockUnit() {
        when(stockMapper.deleteInStockByExternalIdAndUserId(STOCK_EXTERNAL_ID, user.getId())).thenReturn(1);

        stockService.deleteUnit(STOCK_EXTERNAL_ID, USERNAME);

        verify(stockMapper).deleteInStockByExternalIdAndUserId(STOCK_EXTERNAL_ID, user.getId());
    }

    @Test
    void deleteUnit_rejectsMissingUnauthorizedOrNonInStockUnit() {
        when(stockMapper.deleteInStockByExternalIdAndUserId(STOCK_EXTERNAL_ID, user.getId())).thenReturn(0);

        assertThatThrownBy(() -> stockService.deleteUnit(STOCK_EXTERNAL_ID, USERNAME))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("삭제 가능한 재고");
    }

    @Test
    void findMemoSuggestions_returnsFrequentUserMemos() {
        when(transactionMapper.findFrequentMemosByUserIdAndType(user.getId(), TransactionType.OUT, 4))
                .thenReturn(List.of("사용 출고", "폐기 출고"));

        List<String> suggestions = stockService.findMemoSuggestions(TransactionType.OUT, USERNAME);

        assertThat(suggestions).containsExactly("사용 출고", "폐기 출고");
    }

    @Test
    void findMemoSuggestions_fallsBackToMasterMemosWhenUserHasNoHistory() {
        when(transactionMapper.findFrequentMemosByUserIdAndType(user.getId(), TransactionType.IN, 4))
                .thenReturn(List.of());

        List<String> suggestions = stockService.findMemoSuggestions(TransactionType.IN, USERNAME);

        assertThat(suggestions).containsExactly("구매 입고", "반품 입고", "재고 발견", "수량 보정");
    }

    @Test
    void create_happyPath_spaceOnly() {
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        doAnswer(invocation -> { ((StockDTO) invocation.getArgument(0)).setId(501L); return null; })
                .when(stockMapper).insertStock(any());

        stockService.create(stockForm(ITEM_EXTERNAL_ID, SPACE_EXTERNAL_ID, null, null), USERNAME);

        verify(stockMapper).insertStock(any());
        verify(transactionMapper).insertTransaction(any());
    }

    @Test
    void addUnits_insertsStockAndTransactionForEachUnit() {
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        doAnswer(invocation -> { ((StockDTO) invocation.getArgument(0)).setId(501L); return null; })
                .when(stockMapper).insertStock(any());

        StockInOutForm form = stockInOutForm(SPACE_EXTERNAL_ID, null, null);
        form.setCount(3);
        stockService.addUnits(form, USERNAME);

        verify(stockMapper, times(3)).insertStock(any());
        verify(transactionMapper, times(3)).insertTransaction(any());
    }

    @Test
    void addUnits_rejectsItemOwnedByAnotherUser() {
        when(itemMapper.findByExternalId(OTHER_ITEM_EXTERNAL_ID)).thenReturn(Optional.of(otherUserItem));

        StockInOutForm form = stockInOutForm(SPACE_EXTERNAL_ID, null, null);
        form.setItemExternalId(OTHER_ITEM_EXTERNAL_ID);
        assertThatThrownBy(() -> stockService.addUnits(form, USERNAME))
                .isInstanceOf(SecurityException.class);

        verify(stockMapper, never()).insertStock(any());
    }

    @Test
    void dispatchUnits_updatesStatusAndRecordsTransaction() {
        StockDTO unit = new StockDTO();
        unit.setId(700L);
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(stockMapper.findInStockByItemAndSpace(item.getId(), space.getId())).thenReturn(List.of(unit));
        when(stockMapper.updateStatusIfInStock(unit.getId(), StockStatus.DISPATCHED)).thenReturn(1);

        stockService.dispatchUnits(stockInOutForm(SPACE_EXTERNAL_ID, null, null), USERNAME);

        verify(stockMapper).updateStatusIfInStock(unit.getId(), StockStatus.DISPATCHED);
        ArgumentCaptor<StockTransactionDTO> txCaptor = ArgumentCaptor.forClass(StockTransactionDTO.class);
        verify(transactionMapper).insertTransaction(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo(TransactionType.OUT);
    }

    @Test
    void dispatchUnits_rejectsInsufficientStock() {
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(stockMapper.findInStockByItemAndSpace(item.getId(), space.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> stockService.dispatchUnits(stockInOutForm(SPACE_EXTERNAL_ID, null, null), USERNAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("재고가 부족");

        verify(transactionMapper, never()).insertTransaction(any());
    }

    @Test
    void moveUnits_updatesLocationAndRecordsMoveTransaction() {
        StockDTO unit1 = stock(701L);
        StockDTO unit2 = stock(702L);
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(shelfMapper.findByExternalId(SHELF_EXTERNAL_ID)).thenReturn(Optional.of(shelf));
        when(boxMapper.findByExternalId(BOX_EXTERNAL_ID)).thenReturn(Optional.of(box));
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(stockMapper.findInStockByItemAndSpace(item.getId(), space.getId())).thenReturn(List.of(unit1, unit2));
        when(stockMapper.updateLocationIfInStock(List.of(unit1.getId(), unit2.getId()), space.getId(), shelf.getId(), box.getId()))
                .thenReturn(2);

        stockService.moveUnits(stockMoveForm(SPACE_EXTERNAL_ID, null, null, SPACE_EXTERNAL_ID, SHELF_EXTERNAL_ID, BOX_EXTERNAL_ID, 2), USERNAME);

        verify(stockMapper).updateLocationIfInStock(List.of(unit1.getId(), unit2.getId()), space.getId(), shelf.getId(), box.getId());
        ArgumentCaptor<StockTransactionDTO> txCaptor = ArgumentCaptor.forClass(StockTransactionDTO.class);
        verify(transactionMapper, times(2)).insertTransaction(txCaptor.capture());
        assertThat(txCaptor.getAllValues())
                .extracting(StockTransactionDTO::getTransactionType)
                .containsOnly(TransactionType.MOVE);
        assertThat(txCaptor.getAllValues())
                .allSatisfy(tx -> {
                    assertThat(tx.getFromSpaceId()).isEqualTo(space.getId());
                    assertThat(tx.getFromShelfId()).isNull();
                    assertThat(tx.getFromBoxId()).isNull();
                    assertThat(tx.getToSpaceId()).isEqualTo(space.getId());
                    assertThat(tx.getToShelfId()).isEqualTo(shelf.getId());
                    assertThat(tx.getToBoxId()).isEqualTo(box.getId());
                });
    }

    @Test
    void moveUnits_rejectsSameLocation() {
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));

        assertThatThrownBy(() -> stockService.moveUnits(
                stockMoveForm(SPACE_EXTERNAL_ID, null, null, SPACE_EXTERNAL_ID, null, null, 1), USERNAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("같은 위치");

        verify(stockMapper, never()).updateLocationIfInStock(any(), any(), any(), any());
    }

    @Test
    void moveUnits_rejectsInsufficientStock() {
        StockDTO unit = stock(701L);
        when(spaceMapper.findByExternalId(SPACE_EXTERNAL_ID)).thenReturn(Optional.of(space));
        when(shelfMapper.findByExternalId(SHELF_EXTERNAL_ID)).thenReturn(Optional.of(shelf));
        when(itemMapper.findByExternalId(ITEM_EXTERNAL_ID)).thenReturn(Optional.of(item));
        when(stockMapper.findInStockByItemAndSpace(item.getId(), space.getId())).thenReturn(List.of(unit));

        assertThatThrownBy(() -> stockService.moveUnits(
                stockMoveForm(SPACE_EXTERNAL_ID, null, null, SPACE_EXTERNAL_ID, SHELF_EXTERNAL_ID, null, 2), USERNAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("재고가 부족");

        verify(stockMapper, never()).updateLocationIfInStock(any(), any(), any(), any());
        verify(transactionMapper, never()).insertTransaction(any());
    }

    @Test
    void updateDetails_trimsBlankValuesAndReturnsUpdatedStock() {
        StockUpdateForm form = new StockUpdateForm();
        form.setSerialNumber(" SN-1 ");
        form.setLotNumber(" ");
        form.setMemo("  별도 보관  ");
        StockDetailDTO updated = new StockDetailDTO();
        updated.setExternalId(STOCK_EXTERNAL_ID);
        updated.setSerialNumber("SN-1");
        updated.setMemo("별도 보관");

        when(stockMapper.updateDetails(eq(STOCK_EXTERNAL_ID), eq(user.getId()), same(form))).thenReturn(1);
        when(stockMapper.findDetailByExternalId(STOCK_EXTERNAL_ID, user.getId())).thenReturn(Optional.of(updated));

        StockDetailDTO result = stockService.updateDetails(STOCK_EXTERNAL_ID, form, USERNAME);

        assertThat(result.getSerialNumber()).isEqualTo("SN-1");
        assertThat(result.getMemo()).isEqualTo("별도 보관");
        assertThat(form.getSerialNumber()).isEqualTo("SN-1");
        assertThat(form.getLotNumber()).isNull();
        assertThat(form.getMemo()).isEqualTo("별도 보관");
    }

    @Test
    void updateDetails_rejectsMissingOrUnauthorizedStock() {
        StockUpdateForm form = new StockUpdateForm();
        when(stockMapper.updateDetails(eq(STOCK_EXTERNAL_ID), eq(user.getId()), same(form))).thenReturn(0);

        assertThatThrownBy(() -> stockService.updateDetails(STOCK_EXTERNAL_ID, form, USERNAME))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("수정 가능한 재고");
    }

    private StockForm stockForm(UUID itemExternalId, UUID spaceExternalId, UUID shelfExternalId, UUID boxExternalId) {
        StockForm form = new StockForm();
        form.setItemExternalId(itemExternalId);
        form.setSpaceExternalId(spaceExternalId);
        form.setShelfExternalId(shelfExternalId);
        form.setBoxExternalId(boxExternalId);
        form.setCount(1);
        return form;
    }

    private StockInOutForm stockInOutForm(UUID spaceExternalId, UUID shelfExternalId, UUID boxExternalId) {
        StockInOutForm form = new StockInOutForm();
        form.setItemExternalId(ITEM_EXTERNAL_ID);
        form.setSpaceExternalId(spaceExternalId);
        form.setShelfExternalId(shelfExternalId);
        form.setBoxExternalId(boxExternalId);
        form.setCount(1);
        return form;
    }

    private StockMoveForm stockMoveForm(UUID sourceSpaceExternalId,
                                        UUID sourceShelfExternalId,
                                        UUID sourceBoxExternalId,
                                        UUID targetSpaceExternalId,
                                        UUID targetShelfExternalId,
                                        UUID targetBoxExternalId,
                                        int count) {
        StockMoveForm form = new StockMoveForm();
        form.setSourceSpaceExternalId(sourceSpaceExternalId);
        form.setSourceShelfExternalId(sourceShelfExternalId);
        form.setSourceBoxExternalId(sourceBoxExternalId);
        form.setTargetSpaceExternalId(targetSpaceExternalId);
        form.setTargetShelfExternalId(targetShelfExternalId);
        form.setTargetBoxExternalId(targetBoxExternalId);
        StockMoveForm.Item moveItem = new StockMoveForm.Item();
        moveItem.setItemExternalId(ITEM_EXTERNAL_ID);
        moveItem.setCount(count);
        form.setItems(List.of(moveItem));
        return form;
    }

    private StockDTO stock(Long id) {
        StockDTO stock = new StockDTO();
        stock.setId(id);
        return stock;
    }

    private UserDTO user(Long id) {
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setUsername(USERNAME);
        return dto;
    }

    private ItemDTO item(Long id, UUID externalId, Long userId) {
        ItemDTO dto = new ItemDTO();
        dto.setId(id);
        dto.setExternalId(externalId);
        dto.setUserId(userId);
        dto.setActive(true);
        return dto;
    }

    private SpaceDTO space(Long id, UUID externalId, Long userId) {
        SpaceDTO dto = new SpaceDTO();
        dto.setId(id);
        dto.setExternalId(externalId);
        dto.setUserId(userId);
        return dto;
    }

    private ShelfDTO shelf(Long id, UUID externalId, Long spaceId) {
        ShelfDTO dto = new ShelfDTO();
        dto.setId(id);
        dto.setExternalId(externalId);
        dto.setSpaceId(spaceId);
        return dto;
    }

    private BoxDTO box(Long id, UUID externalId, Long shelfId) {
        BoxDTO dto = new BoxDTO();
        dto.setId(id);
        dto.setExternalId(externalId);
        dto.setShelfId(shelfId);
        return dto;
    }
}
