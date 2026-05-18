package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.BoxDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface BoxMapper {
    void insertBox(BoxDTO box);
    Optional<BoxDTO> findById(Long id);
    List<BoxDTO> findByShelfId(Long shelfId);
    void updateBox(BoxDTO box);
    void deleteById(Long id);
}
