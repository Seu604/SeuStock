package com.seu.seustock.service;

import com.seu.seustock.mapper.ShelfMapper;
import com.seu.seustock.mapper.SpaceMapper;
import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.ShelfDTO;
import com.seu.seustock.model.dto.SpaceDTO;
import com.seu.seustock.model.dto.UserDTO;
import com.seu.seustock.model.form.ShelfForm;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShelfService {

    private final ShelfMapper shelfMapper;
    private final SpaceMapper spaceMapper;
    private final UserMapper userMapper;
    private final MessageSource messageSource;

    private String getMsg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    public List<ShelfDTO> findAllBySpaceId(UUID spaceExternalId, String username) {
        SpaceDTO space = getVerifiedSpace(spaceExternalId, username);
        return shelfMapper.findBySpaceId(space.getId());
    }

    public ShelfDTO findByExternalId(UUID spaceExternalId, UUID shelfExternalId, String username) {
        SpaceDTO space = getVerifiedSpace(spaceExternalId, username);
        ShelfDTO shelf = getShelf(shelfExternalId);
        verifyShelfOwnership(shelf, space);
        return shelf;
    }

    public ShelfDTO findById(Long id) {
        return shelfMapper.findById(id)
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.shelf.notFound")));
    }

    public ShelfDTO findByExternalIdOnly(UUID externalId) {
        return getShelf(externalId);
    }

    public ShelfDTO create(UUID spaceExternalId, ShelfForm form, String username) {
        SpaceDTO space = getVerifiedSpace(spaceExternalId, username);
        ShelfDTO shelf = new ShelfDTO();
        shelf.setSpaceId(space.getId());
        shelf.setName(form.getName());
        shelfMapper.insertShelf(shelf);
        return shelfMapper.findById(shelf.getId()).orElseThrow();
    }

    public ShelfDTO rename(UUID spaceExternalId, UUID shelfExternalId, ShelfForm form, String username) {
        SpaceDTO space = getVerifiedSpace(spaceExternalId, username);
        ShelfDTO shelf = getShelf(shelfExternalId);
        verifyShelfOwnership(shelf, space);
        shelf.setName(form.getName());
        shelfMapper.updateShelf(shelf);
        return shelfMapper.findById(shelf.getId()).orElseThrow();
    }

    public void delete(UUID spaceExternalId, UUID shelfExternalId, String username) {
        SpaceDTO space = getVerifiedSpace(spaceExternalId, username);
        ShelfDTO shelf = getShelf(shelfExternalId);
        verifyShelfOwnership(shelf, space);
        shelfMapper.deleteById(shelf.getId());
    }

    SpaceDTO getVerifiedSpace(UUID spaceExternalId, String username) {
        SpaceDTO space = spaceMapper.findByExternalId(spaceExternalId)
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.space.notFound")));
        UserDTO user = userMapper.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.user.notFound")));
        if (!space.getUserId().equals(user.getId())) {
            throw new SecurityException(getMsg("error.403.title"));
        }
        return space;
    }

    private ShelfDTO getShelf(UUID shelfExternalId) {
        return shelfMapper.findByExternalId(shelfExternalId)
                .orElseThrow(() -> new NoSuchElementException(getMsg("error.shelf.notFound")));
    }

    private void verifyShelfOwnership(ShelfDTO shelf, SpaceDTO space) {
        if (!shelf.getSpaceId().equals(space.getId())) {
            throw new SecurityException(getMsg("error.403.title"));
        }
    }
}
