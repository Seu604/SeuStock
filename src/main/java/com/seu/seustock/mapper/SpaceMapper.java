package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.SpaceDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface SpaceMapper {
    void insertSpace(SpaceDTO space);
    Optional<SpaceDTO> findById(Long id);
    Optional<SpaceDTO> findByExternalId(UUID externalId);
    List<SpaceDTO> findByUserId(Long userId);
    List<SpaceDTO> findByUserIdWithOptions(@Param("userId") Long userId,
                                           @Param("keyword") String keyword,
                                           @Param("sortBy") String sortBy);
    void updateSpace(SpaceDTO space);
    void deleteById(Long id);
}
