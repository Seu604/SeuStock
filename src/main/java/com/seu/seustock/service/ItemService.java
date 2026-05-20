package com.seu.seustock.service;

import com.seu.seustock.mapper.ItemImageMapper;
import com.seu.seustock.mapper.ItemMapper;
import com.seu.seustock.mapper.StockMapper;
import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.ImageDTO;
import com.seu.seustock.model.dto.ItemDTO;
import com.seu.seustock.model.dto.UserDTO;
import com.seu.seustock.model.form.ItemForm;
import lombok.RequiredArgsConstructor;
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

    public List<ItemDTO> findAllByUsername(String username) {
        UserDTO user = getUser(username);
        return itemMapper.findByUserId(user.getId());
    }

    public ItemDTO findByExternalId(UUID externalId, String username) {
        ItemDTO item = getItem(externalId);
        verifyOwner(item, username);
        return item;
    }

    @Transactional
    public void create(String username, ItemForm form) {
        UserDTO user = getUser(username);
        ItemDTO item = new ItemDTO();
        item.setUserId(user.getId());
        item.setName(form.getName());
        item.setDescription(form.getDescription());
        itemMapper.insertItem(item);
        attachPrimaryImageIfPresent(item.getId(), user, form);
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

    @Transactional
    public void delete(UUID externalId, String username) {
        ItemDTO item = getItem(externalId);
        verifyOwner(item, username);
        if (!stockMapper.findByItemId(item.getId()).isEmpty()) {
            throw new IllegalStateException("재고가 있는 품목은 삭제할 수 없습니다.");
        }
        itemMapper.deleteById(item.getId());
    }

    private UserDTO getUser(String username) {
        return userMapper.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
    }

    private ItemDTO getItem(UUID externalId) {
        return itemMapper.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("품목을 찾을 수 없습니다."));
    }

    private void verifyOwner(ItemDTO item, String username) {
        UserDTO user = getUser(username);
        if (!item.getUserId().equals(user.getId())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
    }

    private void attachPrimaryImageIfPresent(Long itemId, UserDTO user, ItemForm form) {
        ImageDTO image = imageStorageService.store(form.getImageFile(), user, form.getImageHash());
        if (image == null) {
            return;
        }
        itemImageMapper.unsetPrimaryByItemId(itemId);
        itemImageMapper.insertItemImage(itemId, image.getId(), 0, true);
    }
}
