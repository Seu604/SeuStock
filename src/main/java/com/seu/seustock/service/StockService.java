package com.seu.seustock.service;

import com.seu.seustock.mapper.*;
import com.seu.seustock.model.dto.*;
import com.seu.seustock.model.form.StockForm;
import com.seu.seustock.model.form.StockInOutForm;
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
    private final SpaceMapper spaceMapper;
    private final ShelfMapper shelfMapper;
    private final BoxMapper boxMapper;
    private final UserMapper userMapper;

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

    @Transactional
    public StockPanelDTO create(StockForm form, String username) {
        UserDTO user = getUser(username);
        ItemDTO item = itemMapper.findByExternalId(form.getItemExternalId())
                .orElseThrow(() -> new NoSuchElementException("품목을 찾을 수 없습니다."));
        if (!item.getUserId().equals(user.getId())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
        SpaceDTO space = spaceMapper.findByExternalId(form.getSpaceExternalId())
                .orElseThrow(() -> new NoSuchElementException("공간을 찾을 수 없습니다."));
        if (!space.getUserId().equals(user.getId())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }

        StockDTO stock = new StockDTO();
        stock.setItemId(item.getId());
        stock.setSpaceId(space.getId());
        stock.setQuantity(form.getQuantity());

        if (form.getShelfExternalId() != null) {
            ShelfDTO shelf = shelfMapper.findByExternalId(form.getShelfExternalId())
                    .orElseThrow(() -> new NoSuchElementException("선반을 찾을 수 없습니다."));
            stock.setShelfId(shelf.getId());
        }
        if (form.getBoxExternalId() != null) {
            BoxDTO box = boxMapper.findByExternalId(form.getBoxExternalId())
                    .orElseThrow(() -> new NoSuchElementException("박스를 찾을 수 없습니다."));
            stock.setBoxId(box.getId());
        }

        stockMapper.insertStock(stock);

        if (stock.getQuantity() > 0) {
            StockTransactionDTO tx = new StockTransactionDTO();
            tx.setStockId(stock.getId());
            tx.setTransactionType("IN");
            tx.setQuantityDelta(stock.getQuantity());
            tx.setMemo("초기 등록");
            transactionMapper.insertTransaction(tx);
        }

        return toPanel(stockMapper.findById(stock.getId()).orElseThrow(), item);
    }

    public void delete(UUID stockExternalId, String username) {
        StockDTO stock = getVerifiedStock(stockExternalId, username);
        stockMapper.deleteById(stock.getId());
    }

    @Transactional
    public StockPanelDTO processIn(UUID stockExternalId, StockInOutForm form, String username) {
        StockDTO stock = getVerifiedStock(stockExternalId, username);
        int newQty = stock.getQuantity() + form.getQuantity();

        StockTransactionDTO tx = new StockTransactionDTO();
        tx.setStockId(stock.getId());
        tx.setTransactionType("IN");
        tx.setQuantityDelta(form.getQuantity());
        tx.setMemo(form.getMemo());
        transactionMapper.insertTransaction(tx);

        stockMapper.updateQuantity(stock.getId(), newQty);

        ItemDTO item = itemMapper.findById(stock.getItemId()).orElseThrow();
        stock.setQuantity(newQty);
        return toPanel(stock, item);
    }

    @Transactional
    public StockPanelDTO processOut(UUID stockExternalId, StockInOutForm form, String username) {
        StockDTO stock = getVerifiedStock(stockExternalId, username);
        int newQty = stock.getQuantity() - form.getQuantity();
        if (newQty < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        StockTransactionDTO tx = new StockTransactionDTO();
        tx.setStockId(stock.getId());
        tx.setTransactionType("OUT");
        tx.setQuantityDelta(-form.getQuantity());
        tx.setMemo(form.getMemo());
        transactionMapper.insertTransaction(tx);

        stockMapper.updateQuantity(stock.getId(), newQty);

        ItemDTO item = itemMapper.findById(stock.getItemId()).orElseThrow();
        stock.setQuantity(newQty);
        return toPanel(stock, item);
    }

    private StockDTO getVerifiedStock(UUID stockExternalId, String username) {
        StockDTO stock = stockMapper.findByExternalId(stockExternalId)
                .orElseThrow(() -> new NoSuchElementException("재고를 찾을 수 없습니다."));
        SpaceDTO space = spaceMapper.findById(stock.getSpaceId())
                .orElseThrow(() -> new NoSuchElementException("공간을 찾을 수 없습니다."));
        UserDTO user = getUser(username);
        if (!space.getUserId().equals(user.getId())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
        return stock;
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

    private StockPanelDTO toPanel(StockDTO stock, ItemDTO item) {
        StockPanelDTO dto = new StockPanelDTO();
        dto.setId(stock.getId());
        dto.setExternalId(stock.getExternalId());
        dto.setItemName(item.getName());
        dto.setQuantity(stock.getQuantity());
        return dto;
    }
}
