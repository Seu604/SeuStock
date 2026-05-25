package com.seu.seustock.service;

import com.seu.seustock.mapper.*;
import com.seu.seustock.model.StockStatus;
import com.seu.seustock.model.TransactionType;
import com.seu.seustock.model.dto.*;
import com.seu.seustock.model.form.QuickStockForm;
import com.seu.seustock.model.form.StockForm;
import com.seu.seustock.model.form.StockInOutForm;
import com.seu.seustock.model.form.StockUpdateForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockMapper stockMapper;
    private final StockTransactionMapper transactionMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final SpaceMapper spaceMapper;
    private final ShelfMapper shelfMapper;
    private final BoxMapper boxMapper;
    private final UserMapper userMapper;
    private final ImageStorageService imageStorageService;

    private record VerifiedLocation(SpaceDTO space, ShelfDTO shelf, BoxDTO box) {
        Long shelfId() {
            return shelf == null ? null : shelf.getId();
        }

        Long boxId() {
            return box == null ? null : box.getId();
        }
    }

    public List<StockPanelDTO> findPanelBySpace(UUID spaceExternalId, String username) {
        SpaceDTO space = getVerifiedSpace(spaceExternalId, username);
        return stockMapper.findPanelBySpaceDirectOnly(space.getId());
    }

    public List<StockPanelDTO> findPanelByShelf(UUID spaceExternalId, UUID shelfExternalId, String username) {
        SpaceDTO space = getVerifiedSpace(spaceExternalId, username);
        ShelfDTO shelf = shelfMapper.findByExternalId(shelfExternalId)
                .orElseThrow(() -> new NoSuchElementException("선반을 찾을 수 없습니다."));
        if (!shelf.getSpaceId().equals(space.getId())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
        return stockMapper.findPanelByShelfDirectOnly(shelf.getId());
    }

    public List<StockPanelDTO> findPanelByBox(UUID spaceExternalId, UUID shelfExternalId, UUID boxExternalId, String username) {
        SpaceDTO space = getVerifiedSpace(spaceExternalId, username);
        ShelfDTO shelf = shelfMapper.findByExternalId(shelfExternalId)
                .orElseThrow(() -> new NoSuchElementException("선반을 찾을 수 없습니다."));
        if (!shelf.getSpaceId().equals(space.getId())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
        BoxDTO box = boxMapper.findByExternalId(boxExternalId)
                .orElseThrow(() -> new NoSuchElementException("박스를 찾을 수 없습니다."));
        if (!box.getShelfId().equals(shelf.getId())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
        return stockMapper.findPanelByBoxId(box.getId());
    }

    public List<StockDetailDTO> searchDetails(UUID itemExternalId,
                                              UUID spaceExternalId,
                                              UUID shelfExternalId,
                                              UUID boxExternalId,
                                              String username) {
        UserDTO user = getUser(username);
        return stockMapper.searchDetails(user.getId(), itemExternalId, spaceExternalId, shelfExternalId, boxExternalId);
    }

    public StockDetailDTO findDetailByExternalId(UUID externalId, String username) {
        UserDTO user = getUser(username);
        return stockMapper.findDetailByExternalId(externalId, user.getId())
                .orElseThrow(() -> new NoSuchElementException("재고를 찾을 수 없습니다."));
    }

    @Transactional
    public StockDetailDTO updateDetails(UUID externalId, StockUpdateForm form, String username) {
        UserDTO user = getUser(username);
        normalize(form);
        int updated = stockMapper.updateDetails(externalId, user.getId(), form);
        if (updated != 1) {
            throw new NoSuchElementException("수정 가능한 재고를 찾을 수 없습니다.");
        }
        return stockMapper.findDetailByExternalId(externalId, user.getId())
                .orElseThrow(() -> new NoSuchElementException("재고를 찾을 수 없습니다."));
    }

    @Transactional
    public void create(StockForm form, String username) {
        ItemDTO item = getVerifiedItem(form.getItemExternalId(), username);
        VerifiedLocation location = resolveVerifiedLocation(
                form.getSpaceExternalId(),
                form.getShelfExternalId(),
                form.getBoxExternalId(),
                username);

        for (int i = 0; i < form.getCount(); i++) {
            StockDTO unit = new StockDTO();
            unit.setItemId(item.getId());
            unit.setSpaceId(location.space().getId());
            unit.setShelfId(location.shelfId());
            unit.setBoxId(location.boxId());
            unit.setSerialNumber(form.getCount() == 1 ? form.getSerialNumber() : null);
            unit.setLotNumber(form.getLotNumber());
            unit.setExpirationDate(form.getExpirationDate());
            stockMapper.insertStock(unit);

            StockTransactionDTO tx = new StockTransactionDTO();
            tx.setStockId(unit.getId());
            tx.setTransactionType(TransactionType.IN);
            tx.setMemo(form.getMemo() != null ? form.getMemo() : "초기 등록");
            transactionMapper.insertTransaction(tx);
        }
    }

    @Transactional
    public void createWithNewItem(QuickStockForm form, String username) {
        UserDTO user = getUser(username);

        ItemDTO item = new ItemDTO();
        item.setUserId(user.getId());
        item.setName(form.getName());
        item.setDescription(form.getDescription());
        itemMapper.insertItem(item);
        attachPrimaryImageIfPresent(item.getId(), user, form);

        VerifiedLocation location = resolveVerifiedLocation(
                form.getSpaceExternalId(), form.getShelfExternalId(), form.getBoxExternalId(), username);

        for (int i = 0; i < form.getCount(); i++) {
            StockDTO unit = new StockDTO();
            unit.setItemId(item.getId());
            unit.setSpaceId(location.space().getId());
            unit.setShelfId(location.shelfId());
            unit.setBoxId(location.boxId());
            stockMapper.insertStock(unit);

            StockTransactionDTO tx = new StockTransactionDTO();
            tx.setStockId(unit.getId());
            tx.setTransactionType(TransactionType.IN);
            tx.setMemo(form.getMemo() != null ? form.getMemo() : "빠른 등록");
            transactionMapper.insertTransaction(tx);
        }
    }

    @Transactional
    public void addUnits(StockInOutForm form, String username) {
        ItemDTO item = getVerifiedItem(form.getItemExternalId(), username);
        VerifiedLocation location = resolveVerifiedLocation(
                form.getSpaceExternalId(),
                form.getShelfExternalId(),
                form.getBoxExternalId(),
                username);

        for (int i = 0; i < form.getCount(); i++) {
            StockDTO unit = new StockDTO();
            unit.setItemId(item.getId());
            unit.setSpaceId(location.space().getId());
            unit.setShelfId(location.shelfId());
            unit.setBoxId(location.boxId());
            stockMapper.insertStock(unit);

            StockTransactionDTO tx = new StockTransactionDTO();
            tx.setStockId(unit.getId());
            tx.setTransactionType(TransactionType.IN);
            tx.setMemo(form.getMemo());
            transactionMapper.insertTransaction(tx);
        }
    }

    @Transactional
    public void dispatchUnits(StockInOutForm form, String username) {
        ItemDTO item = getVerifiedItem(form.getItemExternalId(), username);
        VerifiedLocation location = resolveVerifiedLocation(
                form.getSpaceExternalId(),
                form.getShelfExternalId(),
                form.getBoxExternalId(),
                username);

        List<StockDTO> units;
        if (location.box() != null) {
            units = stockMapper.findInStockByItemAndBox(item.getId(), location.box().getId());
        } else if (location.shelf() != null) {
            units = stockMapper.findInStockByItemAndShelf(item.getId(), location.shelf().getId());
        } else {
            units = stockMapper.findInStockByItemAndSpace(item.getId(), location.space().getId());
        }

        if (units.size() < form.getCount()) {
            throw new IllegalArgumentException("재고가 부족합니다. (현재: " + units.size() + "개)");
        }

        for (StockDTO unit : units.subList(0, form.getCount())) {
            int updated = stockMapper.updateStatusIfInStock(unit.getId(), StockStatus.DISPATCHED);
            if (updated != 1) {
                throw new IllegalStateException("재고 상태가 변경되어 출고할 수 없습니다.");
            }

            StockTransactionDTO tx = new StockTransactionDTO();
            tx.setStockId(unit.getId());
            tx.setTransactionType(TransactionType.OUT);
            tx.setMemo(form.getMemo());
            transactionMapper.insertTransaction(tx);
        }
    }

    @Transactional
    public void deleteUnits(UUID itemExternalId, UUID spaceExternalId, UUID shelfExternalId, UUID boxExternalId, String username) {
        ItemDTO item = getVerifiedItem(itemExternalId, username);
        VerifiedLocation location = resolveVerifiedLocation(spaceExternalId, shelfExternalId, boxExternalId, username);

        if (location.box() != null) {
            stockMapper.deleteInStockByItemAndBox(item.getId(), location.box().getId());
        } else if (location.shelf() != null) {
            stockMapper.deleteInStockByItemAndShelf(item.getId(), location.shelf().getId());
        } else {
            stockMapper.deleteInStockByItemAndSpace(item.getId(), location.space().getId());
        }
    }

    private ItemDTO getVerifiedItem(UUID itemExternalId, String username) {
        ItemDTO item = itemMapper.findByExternalId(itemExternalId)
                .orElseThrow(() -> new NoSuchElementException("품목을 찾을 수 없습니다."));
        verifyItemOwner(item, username);
        if (!item.isActive()) {
            throw new IllegalStateException("비활성화된 품목은 재고 작업을 할 수 없습니다.");
        }
        return item;
    }

    private void verifyItemOwner(ItemDTO item, String username) {
        UserDTO user = getUser(username);
        if (!item.getUserId().equals(user.getId())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
    }

    private VerifiedLocation resolveVerifiedLocation(UUID spaceExternalId,
                                                     UUID shelfExternalId,
                                                     UUID boxExternalId,
                                                     String username) {
        SpaceDTO space = getVerifiedSpace(spaceExternalId, username);
        ShelfDTO shelf = null;
        BoxDTO box = null;

        if (boxExternalId != null && shelfExternalId == null) {
            throw new IllegalArgumentException("박스 재고는 선반 정보가 필요합니다.");
        }

        if (shelfExternalId != null) {
            shelf = shelfMapper.findByExternalId(shelfExternalId)
                    .orElseThrow(() -> new NoSuchElementException("선반을 찾을 수 없습니다."));
            if (!shelf.getSpaceId().equals(space.getId())) {
                throw new SecurityException("접근 권한이 없습니다.");
            }
        }

        if (boxExternalId != null) {
            box = boxMapper.findByExternalId(boxExternalId)
                    .orElseThrow(() -> new NoSuchElementException("박스를 찾을 수 없습니다."));
            if (!box.getShelfId().equals(shelf.getId())) {
                throw new SecurityException("접근 권한이 없습니다.");
            }
        }

        return new VerifiedLocation(space, shelf, box);
    }

    private SpaceDTO getVerifiedSpace(UUID spaceExternalId, String username) {
        SpaceDTO space = spaceMapper.findByExternalId(spaceExternalId)
                .orElseThrow(() -> new NoSuchElementException("공간을 찾을 수 없습니다."));
        UserDTO user = getUser(username);
        if (!space.getUserId().equals(user.getId())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
        return space;
    }

    private UserDTO getUser(String username) {
        return userMapper.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
    }

    private void attachPrimaryImageIfPresent(Long itemId, UserDTO user, QuickStockForm form) {
        ImageDTO image = imageStorageService.store(form.getImageFile(), user, form.getImageHash());
        if (image == null) {
            return;
        }
        itemImageMapper.insertItemImage(itemId, image.getId(), 0, true);
    }

    private void normalize(StockUpdateForm form) {
        form.setSerialNumber(blankToNull(form.getSerialNumber()));
        form.setLotNumber(blankToNull(form.getLotNumber()));
        form.setMemo(blankToNull(form.getMemo()));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
