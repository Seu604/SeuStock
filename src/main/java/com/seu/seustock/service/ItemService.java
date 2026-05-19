package com.seu.seustock.service;

import com.seu.seustock.mapper.ItemMapper;
import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.ItemDTO;
import com.seu.seustock.model.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    public List<ItemDTO> findAllByUsername(String username) {
        UserDTO user = userMapper.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        return itemMapper.findByUserId(user.getId());
    }
}
