package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.UserDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface UserMapper {
    void insertUser(UserDTO user);
    Optional<UserDTO> findByUsername(String username);
    void updatePassword(UserDTO user);
    void deleteById(Long id);
}
