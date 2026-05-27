package com.seu.seustock.service;

import com.seu.seustock.mapper.*;
import com.seu.seustock.model.enumeration.StockStatus;
import com.seu.seustock.model.enumeration.TransactionMemoMaster;
import com.seu.seustock.model.enumeration.TransactionType;
import com.seu.seustock.model.dto.*;
import com.seu.seustock.model.form.QuickStockForm;
import com.seu.seustock.model.form.StockForm;
import com.seu.seustock.model.form.StockInOutForm;
import com.seu.seustock.model.form.StockMoveForm;
import com.seu.seustock.model.form.StockUpdateForm;
import com.seu.seustock.model.pagination.PageRequest;
import com.seu.seustock.model.pagination.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

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
    private final MessageSource messageSource;
    private static final int MEMO_SUGGESTION_LIMIT = 4;

    private record VerifiedLocation(SpaceDTO space, ShelfDTO shelf, BoxDTO box) {
        Long shelfId() {
            return shelf == null ? null : shelf.getId();
        }

        Long boxId() {
            return box == null ? null : box.getId();
        }
    }

    private String getMsg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    public List<StockPanelDTO> findPanelBySpace(UUID spaceExternalId, String username) {
        return findPanelPageBySpace(spaceExternalId, username, 1).content();
    }

    public PageResult<StockPanelDTO> findPanelPageBySpace(UUID spaceExternalId, String username, Integer page) {
        UserDTO user = getUser(username);
        SpaceDTO space = getVerifiedSpace(spaceExternalId, user);
        int totalCount = stockMapper.countPanelBySpaceDirectOnly(space.getId());
        PageRequest pageRequest = PageRequest.of(page, totalCount);
        List<StockPanelDTO> stocks = stockMapper.findPanelBySpaceDirectOnlyPaged(
                space.getId(), pageRequest.size(), pageRequest.offset());
        return new PageResult<>(stocks, pageRequest.page(), pageRequest.size(), totalCount);
    }

    public List<StockPanelDTO> findPanelBySpaceAll(UUID spaceExternalId, String keyword, String sortBy, String username) {
        return findPanelPageBySpaceAll(spaceExternalId, keyword, sortBy, username, 1).content();
    }

    public PageResult<StockPanelDTO> findPanelPageBySpaceAll(UUID spaceExternalId, String keyword, String sortBy,
                                                             String username, Integer page) {
        UserDTO user = getUser(username);
        SpaceDTO space = getVerifiedSpace(spaceExternalId, user);
        String effectiveKeyword = normalizeKeyword(keyword);
        int totalCount = stockMapper.countPanelBySpaceAllWithOptions(space.getId(), effectiveKeyword);
        PageRequest pageRequest = PageRequest.of(page, totalCount);
        List<StockPanelDTO> stocks = stockMapper.findPanelBySpaceAllWithOptions(space.getId(), effectiveKeyword,
                normalizeSort(sortBy), pageRequest.size(), pageRequest.offset());
        return new PageResult<>(stocks, pageRequest.page(), pageRequest.size(), totalCount);
    }

    public List<StockPanelDTO> findPanelByShelf(UUID spaceExternalId, UUID shelfExternalId, String username) {
        return findPanelPageByShelf(spaceExternalId, shelfExternalId, username, 1).content();
    }

    public PageResult<StockPanelDTO> findPanelPageByShelf(UUID spaceExternalId, UUID shelfExternalId,
                                                          String username, Integer page) {
        UserDTO user = getUser(username);
        SpaceDTO space = getVerifiedSpace(spaceExternalId, user);
        ShelfDTO shelf = shelfMapper.findByExternalId(shelfExternalId)
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.shelf.notFound")));
        if (!shelf.getSpaceId().equals(space.getId())) {
            throw new SecurityException(getMsg("error.403.title"));
        }
        int totalCount = stockMapper.countPanelByShelfDirectOnly(shelf.getId());
        PageRequest pageRequest = PageRequest.of(page, totalCount);
        List<StockPanelDTO> stocks = stockMapper.findPanelByShelfDirectOnlyPaged(
                shelf.getId(), pageRequest.size(), pageRequest.offset());
        return new PageResult<>(stocks, pageRequest.page(), pageRequest.size(), totalCount);
    }

    public List<StockPanelDTO> findPanelByBox(UUID spaceExternalId, UUID shelfExternalId, UUID boxExternalId, String username) {
        return findPanelPageByBox(spaceExternalId, shelfExternalId, boxExternalId, username, 1).content();
    }

    public PageResult<StockPanelDTO> findPanelPageByBox(UUID spaceExternalId, UUID shelfExternalId, UUID boxExternalId,
                                                        String username, Integer page) {
        UserDTO user = getUser(username);
        SpaceDTO space = getVerifiedSpace(spaceExternalId, user);
        ShelfDTO shelf = shelfMapper.findByExternalId(shelfExternalId)
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.shelf.notFound")));
        if (!shelf.getSpaceId().equals(space.getId())) {
            throw new SecurityException(getMsg("error.403.title"));
        }
        BoxDTO box = boxMapper.findByExternalId(boxExternalId)
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.box.notFound")));
        if (!box.getShelfId().equals(shelf.getId())) {
            throw new SecurityException(getMsg("error.403.title"));
        }
        int totalCount = stockMapper.countPanelByBoxId(box.getId());
        PageRequest pageRequest = PageRequest.of(page, totalCount);
        List<StockPanelDTO> stocks = stockMapper.findPanelByBoxIdPaged(
                box.getId(), pageRequest.size(), pageRequest.offset());
        return new PageResult<>(stocks, pageRequest.page(), pageRequest.size(), totalCount);
    }

    public List<StockDetailDTO> searchDetails(UUID itemExternalId,
                                              UUID spaceExternalId,
                                              UUID shelfExternalId,
                                              UUID boxExternalId,
                                              String keyword,
                                              String sortBy,
                                              String username) {
        return searchDetailsPage(itemExternalId, spaceExternalId, shelfExternalId, boxExternalId,
                keyword, sortBy, username, 1).content();
    }

    public PageResult<StockDetailDTO> searchDetailsPage(UUID itemExternalId,
                                                        UUID spaceExternalId,
                                                        UUID shelfExternalId,
                                                        UUID boxExternalId,
                                                        String keyword,
                                                        String sortBy,
                                                        String username,
                                                        Integer page) {
        UserDTO user = getUser(username);
        String effectiveKeyword = normalizeKeyword(keyword);
        int totalCount = stockMapper.countSearchDetails(user.getId(), itemExternalId, spaceExternalId,
                shelfExternalId, boxExternalId, effectiveKeyword);
        PageRequest pageRequest = PageRequest.of(page, totalCount);
        List<StockDetailDTO> stocks = stockMapper.searchDetails(user.getId(), itemExternalId, spaceExternalId,
                shelfExternalId, boxExternalId, effectiveKeyword, normalizeSort(sortBy),
                pageRequest.size(), pageRequest.offset());
        return new PageResult<>(stocks, pageRequest.page(), pageRequest.size(), totalCount);
    }

    public StockDetailDTO findDetailByExternalId(UUID externalId, String username) {
        UserDTO user = getUser(username);
        return stockMapper.findDetailByExternalId(externalId, user.getId())
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.stock.notFound")));
    }

    public List<String> findMemoSuggestions(TransactionType transactionType, String username) {
        UserDTO user = getUser(username);
        List<String> frequentMemos = transactionMapper.findFrequentMemosByUserIdAndType(
                user.getId(), transactionType, MEMO_SUGGESTION_LIMIT);
        return Stream.concat(frequentMemos.stream(), TransactionMemoMaster.memosFor(transactionType).stream())
                .distinct()
                .limit(MEMO_SUGGESTION_LIMIT)
                .toList();
    }

    @Transactional
    public StockDetailDTO updateDetails(UUID externalId, StockUpdateForm form, String username) {
        UserDTO user = getUser(username);
        normalize(form);
        int updated = stockMapper.updateDetails(externalId, user.getId(), form);
        if (updated != 1) {
            throw new NoSuchElementException(getMsg("error.stock.notFound"));
        }
        return stockMapper.findDetailByExternalId(externalId, user.getId())
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.stock.notFound")));
    }

    @Transactional
    public void create(StockForm form, String username) {
        UserDTO user = getUser(username);
        ItemDTO item = getVerifiedItem(form.getItemExternalId(), user);
        VerifiedLocation location = resolveVerifiedLocation(
                form.getSpaceExternalId(),
                form.getShelfExternalId(),
                form.getBoxExternalId(),
                user);

        List<StockDTO> units = new ArrayList<>(form.getCount());
        for (int i = 0; i < form.getCount(); i++) {
            StockDTO unit = new StockDTO();
            unit.setItemId(item.getId());
            unit.setSpaceId(location.space().getId());
            unit.setShelfId(location.shelfId());
            unit.setBoxId(location.boxId());
            unit.setSerialNumber(form.getCount() == 1 ? form.getSerialNumber() : null);
            unit.setLotNumber(form.getLotNumber());
            unit.setExpirationDate(form.getExpirationDate());
            units.add(unit);
        }
        stockMapper.insertStocks(units);

        String memo = form.getMemo() != null ? form.getMemo() : "초기 등록";
        List<StockTransactionDTO> txs = new ArrayList<>(units.size());
        for (StockDTO unit : units) {
            StockTransactionDTO tx = new StockTransactionDTO();
            tx.setStockId(unit.getId());
            tx.setTransactionType(TransactionType.IN);
            tx.setMemo(memo);
            txs.add(tx);
        }
        transactionMapper.insertTransactions(txs);
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
                form.getSpaceExternalId(), form.getShelfExternalId(), form.getBoxExternalId(), user);

        List<StockDTO> units = new ArrayList<>(form.getCount());
        for (int i = 0; i < form.getCount(); i++) {
            StockDTO unit = new StockDTO();
            unit.setItemId(item.getId());
            unit.setSpaceId(location.space().getId());
            unit.setShelfId(location.shelfId());
            unit.setBoxId(location.boxId());
            units.add(unit);
        }
        stockMapper.insertStocks(units);

        String memo = form.getMemo() != null ? form.getMemo() : "빠른 등록";
        List<StockTransactionDTO> txs = new ArrayList<>(units.size());
        for (StockDTO unit : units) {
            StockTransactionDTO tx = new StockTransactionDTO();
            tx.setStockId(unit.getId());
            tx.setTransactionType(TransactionType.IN);
            tx.setMemo(memo);
            txs.add(tx);
        }
        transactionMapper.insertTransactions(txs);
    }

    @Transactional
    public void addUnits(StockInOutForm form, String username) {
        UserDTO user = getUser(username);
        ItemDTO item = getVerifiedItem(form.getItemExternalId(), user);
        VerifiedLocation location = resolveVerifiedLocation(
                form.getSpaceExternalId(),
                form.getShelfExternalId(),
                form.getBoxExternalId(),
                user);

        List<StockDTO> units = new ArrayList<>(form.getCount());
        for (int i = 0; i < form.getCount(); i++) {
            StockDTO unit = new StockDTO();
            unit.setItemId(item.getId());
            unit.setSpaceId(location.space().getId());
            unit.setShelfId(location.shelfId());
            unit.setBoxId(location.boxId());
            units.add(unit);
        }
        stockMapper.insertStocks(units);

        List<StockTransactionDTO> txs = new ArrayList<>(units.size());
        for (StockDTO unit : units) {
            StockTransactionDTO tx = new StockTransactionDTO();
            tx.setStockId(unit.getId());
            tx.setTransactionType(TransactionType.IN);
            tx.setMemo(form.getMemo());
            txs.add(tx);
        }
        transactionMapper.insertTransactions(txs);
    }

    @Transactional
    public void dispatchUnits(StockInOutForm form, String username) {
        UserDTO user = getUser(username);
        ItemDTO item = getVerifiedItem(form.getItemExternalId(), user);
        VerifiedLocation location = resolveVerifiedLocation(
                form.getSpaceExternalId(),
                form.getShelfExternalId(),
                form.getBoxExternalId(),
                user);

        List<StockDTO> units;
        if (location.box() != null) {
            units = stockMapper.findInStockByItemAndBox(item.getId(), location.box().getId());
        } else if (location.shelf() != null) {
            units = stockMapper.findInStockByItemAndShelf(item.getId(), location.shelf().getId());
        } else {
            units = stockMapper.findInStockByItemAndSpace(item.getId(), location.space().getId());
        }

        if (units.size() < form.getCount()) {
            throw new IllegalArgumentException(getMsg("error.stock.insufficient", units.size()));
        }

        for (StockDTO unit : units.subList(0, form.getCount())) {
            int updated = stockMapper.updateStatusIfInStock(unit.getId(), StockStatus.DISPATCHED);
            if (updated != 1) {
                throw new IllegalStateException(getMsg("error.stock.statusChanged"));
            }

            StockTransactionDTO tx = new StockTransactionDTO();
            tx.setStockId(unit.getId());
            tx.setTransactionType(TransactionType.OUT);
            tx.setMemo(form.getMemo());
            transactionMapper.insertTransaction(tx);
        }
    }

    @Transactional
    public void moveUnits(StockMoveForm form, String username) {
        UserDTO user = getUser(username);
        VerifiedLocation source = resolveVerifiedLocation(
                form.getSourceSpaceExternalId(),
                form.getSourceShelfExternalId(),
                form.getSourceBoxExternalId(),
                user);
        VerifiedLocation target = resolveVerifiedLocation(
                form.getTargetSpaceExternalId(),
                form.getTargetShelfExternalId(),
                form.getTargetBoxExternalId(),
                user);

        if (isSameLocation(source, target)) {
            throw new IllegalArgumentException(getMsg("error.stock.move.sameLocation"));
        }

        for (StockMoveForm.Item moveItem : form.getItems()) {
            ItemDTO item = getVerifiedItem(moveItem.getItemExternalId(), user);
            List<StockDTO> candidates = findInStockUnits(item.getId(), source);
            if (candidates.size() < moveItem.getCount()) {
                throw new IllegalArgumentException(
                        item.getName() + " " + getMsg("error.stock.insufficient", candidates.size()));
            }

            List<StockDTO> selected = candidates.subList(0, moveItem.getCount());
            List<Long> stockIds = selected.stream().map(StockDTO::getId).toList();
            int updated = stockMapper.updateLocationIfInStock(
                    stockIds, target.space().getId(), target.shelfId(), target.boxId());
            if (updated != stockIds.size()) {
                throw new IllegalStateException(getMsg("error.stock.statusChanged"));
            }

            for (StockDTO unit : selected) {
                StockTransactionDTO tx = new StockTransactionDTO();
                tx.setStockId(unit.getId());
                tx.setTransactionType(TransactionType.MOVE);
                tx.setFromSpaceId(source.space().getId());
                tx.setFromShelfId(source.shelfId());
                tx.setFromBoxId(source.boxId());
                tx.setToSpaceId(target.space().getId());
                tx.setToShelfId(target.shelfId());
                tx.setToBoxId(target.boxId());
                tx.setMemo(form.getMemo());
                transactionMapper.insertTransaction(tx);
            }
        }
    }

    @Transactional
    public void deleteUnits(UUID itemExternalId, UUID spaceExternalId, UUID shelfExternalId, UUID boxExternalId, String username) {
        UserDTO user = getUser(username);
        ItemDTO item = getVerifiedItem(itemExternalId, user);
        VerifiedLocation location = resolveVerifiedLocation(spaceExternalId, shelfExternalId, boxExternalId, user);

        if (location.box() != null) {
            stockMapper.deleteInStockByItemAndBox(item.getId(), location.box().getId());
        } else if (location.shelf() != null) {
            stockMapper.deleteInStockByItemAndShelf(item.getId(), location.shelf().getId());
        } else {
            stockMapper.deleteInStockByItemAndSpace(item.getId(), location.space().getId());
        }
    }

    @Transactional
    public void deleteUnit(UUID stockExternalId, String username) {
        UserDTO user = getUser(username);
        int deleted = stockMapper.deleteInStockByExternalIdAndUserId(stockExternalId, user.getId());
        if (deleted != 1) {
            throw new NoSuchElementException(getMsg("error.stock.notFound"));
        }
    }

    private ItemDTO getVerifiedItem(UUID itemExternalId, UserDTO user) {
        ItemDTO item = itemMapper.findByExternalId(itemExternalId)
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.item.notFound")));
        verifyItemOwner(item, user);
        if (!item.isActive()) {
            throw new IllegalStateException(getMsg("error.item.inactive"));
        }
        return item;
    }

    private void verifyItemOwner(ItemDTO item, UserDTO user) {
        if (!item.getUserId().equals(user.getId())) {
            throw new SecurityException(getMsg("error.403.title"));
        }
    }

    private VerifiedLocation resolveVerifiedLocation(UUID spaceExternalId,
                                                     UUID shelfExternalId,
                                                     UUID boxExternalId,
                                                     UserDTO user) {
        SpaceDTO space = getVerifiedSpace(spaceExternalId, user);
        ShelfDTO shelf = null;
        BoxDTO box = null;

        if (boxExternalId != null && shelfExternalId == null) {
            throw new IllegalArgumentException(getMsg("error.box.requiresShelf"));
        }

        if (shelfExternalId != null) {
            shelf = shelfMapper.findByExternalId(shelfExternalId)
                    .orElseThrow(() -> new NoSuchElementException(getMsg("error.shelf.notFound")));
            if (!shelf.getSpaceId().equals(space.getId())) {
                throw new SecurityException(getMsg("error.403.title"));
            }
        }

        if (boxExternalId != null) {
            box = boxMapper.findByExternalId(boxExternalId)
                    .orElseThrow(() -> new NoSuchElementException(getMsg("error.box.notFound")));
            if (!box.getShelfId().equals(shelf.getId())) {
                throw new SecurityException(getMsg("error.403.title"));
            }
        }

        return new VerifiedLocation(space, shelf, box);
    }

    private List<StockDTO> findInStockUnits(Long itemId, VerifiedLocation location) {
        if (location.box() != null) {
            return stockMapper.findInStockByItemAndBox(itemId, location.box().getId());
        }
        if (location.shelf() != null) {
            return stockMapper.findInStockByItemAndShelf(itemId, location.shelf().getId());
        }
        return stockMapper.findInStockByItemAndSpace(itemId, location.space().getId());
    }

    private boolean isSameLocation(VerifiedLocation source, VerifiedLocation target) {
        return Objects.equals(source.space().getId(), target.space().getId())
                && Objects.equals(source.shelfId(), target.shelfId())
                && Objects.equals(source.boxId(), target.boxId());
    }

    private SpaceDTO getVerifiedSpace(UUID spaceExternalId, UserDTO user) {
        SpaceDTO space = spaceMapper.findByExternalId(spaceExternalId)
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.space.notFound")));
        if (!space.getUserId().equals(user.getId())) {
            throw new SecurityException(getMsg("error.403.title"));
        }
        return space;
    }

    private UserDTO getUser(String username) {
        return userMapper.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.user.notFound")));
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private String normalizeSort(String sortBy) {
        return sortBy == null || sortBy.isBlank() ? "newest" : sortBy;
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
