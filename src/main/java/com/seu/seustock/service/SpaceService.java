package com.seu.seustock.service;

import com.seu.seustock.mapper.SpaceMapper;
import com.seu.seustock.mapper.StockMapper;
import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.SpaceDTO;
import com.seu.seustock.model.dto.UserDTO;
import com.seu.seustock.model.form.SpaceForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceMapper spaceMapper;
    private final UserMapper userMapper;
    private final StockMapper stockMapper;

    public List<SpaceDTO> findAllByUsername(String username) {
        UserDTO user = getUser(username);
        return spaceMapper.findByUserId(user.getId());
    }

    public SpaceDTO findByExternalId(UUID externalId, String username) {
        SpaceDTO space = getSpace(externalId);
        verifyOwner(space, username);
        return space;
    }

    public void create(String username, SpaceForm form) {
        UserDTO user = getUser(username);
        SpaceDTO space = new SpaceDTO();
        space.setUserId(user.getId());
        space.setName(form.getName());
        spaceMapper.insertSpace(space);
    }

    public SpaceDTO update(UUID externalId, SpaceForm form, String username) {
        SpaceDTO space = getSpace(externalId);
        verifyOwner(space, username);
        space.setName(form.getName());
        spaceMapper.updateSpace(space);
        return spaceMapper.findByExternalId(externalId).orElseThrow();
    }

    public void delete(UUID externalId, String username) {
        SpaceDTO space = getSpace(externalId);
        verifyOwner(space, username);
        if (!stockMapper.findBySpaceId(space.getId()).isEmpty()) {
            throw new IllegalStateException("재고가 있는 공간은 삭제할 수 없습니다.");
        }
        spaceMapper.deleteById(space.getId());
    }

    private UserDTO getUser(String username) {
        return userMapper.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
    }

    private SpaceDTO getSpace(UUID externalId) {
        return spaceMapper.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("공간을 찾을 수 없습니다."));
    }

    private void verifyOwner(SpaceDTO space, String username) {
        UserDTO user = getUser(username);
        if (!space.getUserId().equals(user.getId())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
    }
}
