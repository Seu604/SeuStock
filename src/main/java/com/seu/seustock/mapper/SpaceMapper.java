package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.SpaceDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SpaceMapper {
    void insertSpace(SpaceDTO space);
    Optional<SpaceDTO> findById(Long id);
    List<SpaceDTO> findByUserId(Long userId);
    void updateSpace(SpaceDTO space);
    void deleteById(Long id);
}
