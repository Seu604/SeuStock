package com.seu.seustock.service;

import com.seu.seustock.mapper.ItemImageMapper;
import com.seu.seustock.mapper.ItemMapper;
import com.seu.seustock.mapper.StockMapper;
import com.seu.seustock.mapper.StockTransactionMapper;
import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.ImageDTO;
import com.seu.seustock.model.dto.ItemDTO;
import com.seu.seustock.model.dto.ItemSpaceStockDTO;
import com.seu.seustock.model.dto.ItemTransactionHistoryDTO;
import com.seu.seustock.model.dto.UserDTO;
import com.seu.seustock.model.form.ItemForm;
import com.seu.seustock.model.pagination.PageRequest;
import com.seu.seustock.model.pagination.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final StockMapper stockMapper;
    private final ItemImageMapper itemImageMapper;
    private final ImageStorageService imageStorageService;
    private final StockTransactionMapper transactionMapper;
    private final MessageSource messageSource;

    private String getMsg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    public List<ItemDTO> findAllByUsername(String username) {
        UserDTO user = getUser(username);
        return itemMapper.findByUserId(user.getId());
    }

    public List<ItemDTO> findAllByUsername(String username, String keyword, String sortBy) {
        return findPageByUsername(username, keyword, sortBy, 1).content();
    }

    public PageResult<ItemDTO> findPageByUsername(String username, String keyword, String sortBy, Integer page) {
        UserDTO user = getUser(username);
        String effectiveKeyword = normalizeKeyword(keyword);
        int totalCount = itemMapper.countByUserIdWithOptions(user.getId(), effectiveKeyword);
        PageRequest pageRequest = PageRequest.of(page, totalCount);
        List<ItemDTO> items = itemMapper.findByUserIdWithOptions(user.getId(), effectiveKeyword,
                normalizeSort(sortBy), pageRequest.size(), pageRequest.offset());
        return new PageResult<>(items, pageRequest.page(), pageRequest.size(), totalCount);
    }

    public ItemDTO findByExternalId(UUID externalId, String username) {
        ItemDTO item = getItem(externalId);
        verifyOwner(item, username);
        return item;
    }

    @Transactional
    public ItemDTO create(String username, ItemForm form) {
        UserDTO user = getUser(username);
        ItemDTO item = new ItemDTO();
        item.setUserId(user.getId());
        item.setName(form.getName());
        item.setDescription(form.getDescription());
        itemMapper.insertItem(item);
        attachPrimaryImageIfPresent(item.getId(), user, form);
        return itemMapper.findById(item.getId()).orElseThrow();
    }

    @Transactional
    public ItemDTO update(UUID externalId, ItemForm form, String username) {
        ItemDTO item = getItem(externalId);
        verifyOwner(item, username);
        item.setName(form.getName());
        item.setDescription(form.getDescription());
        itemMapper.updateItem(item);
        attachPrimaryImageIfPresent(item.getId(), getUser(username), form);
        return itemMapper.findByExternalId(externalId).orElseThrow();
    }

    public List<ItemSpaceStockDTO> findSpaceStock(UUID itemExternalId, String username) {
        UserDTO user = getUser(username);
        ItemDTO item = getItem(itemExternalId);
        verifyOwner(item, username);
        return stockMapper.findSpaceStockByItem(itemExternalId, user.getId());
    }

    public List<ItemTransactionHistoryDTO> findTransactionHistory(UUID itemExternalId, String username) {
        UserDTO user = getUser(username);
        ItemDTO item = getItem(itemExternalId);
        verifyOwner(item, username);
        return transactionMapper.findHistoryByItemExternalId(itemExternalId, user.getId());
    }

    @Transactional
    public void delete(UUID externalId, String username) {
        ItemDTO item = getItem(externalId);
        verifyOwner(item, username);
        if (stockMapper.countInStockByItemId(item.getId()) > 0) {
            throw new IllegalStateException(getMsg("error.item.hasStock"));
        }
        if (stockMapper.countByItemId(item.getId()) > 0) {
            itemMapper.deactivateById(item.getId());
            return;
        }
        itemMapper.deleteById(item.getId());
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

    private ItemDTO getItem(UUID externalId) {
        return itemMapper.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.item.notFound")));
    }

    private void verifyOwner(ItemDTO item, String username) {
        UserDTO user = getUser(username);
        if (!item.getUserId().equals(user.getId())) {
            throw new SecurityException(getMsg("error.403.title"));
        }
    }

    private void attachPrimaryImageIfPresent(Long itemId, UserDTO user, ItemForm form) {
        ImageDTO image = imageStorageService.store(form.getImageFile(), user, form.getImageHash());
        if (image == null) {
            return;
        }
        itemImageMapper.unsetPrimaryByItemId(itemId);
        if (itemImageMapper.countByItemIdAndImageId(itemId, image.getId()) > 0) {
            itemImageMapper.updateItemImage(itemId, image.getId(), 0, true);
            return;
        }
        itemImageMapper.insertItemImage(itemId, image.getId(), 0, true);
    }
}
