package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.SpaceDTO;
import com.seu.seustock.model.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("classpath:schema-test.sql")
class SpaceMapperTest {

    @Autowired
    private SpaceMapper spaceMapper;

    @Autowired
    private UserMapper userMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        UserDTO user = new UserDTO();
        user.setEmail("testuser@test.com");
        user.setNickname("testuser");
        user.setPassword("password");
        userMapper.insertUser(user);
        userId = user.getId();
    }

    private SpaceDTO buildSpace(String name) {
        SpaceDTO space = new SpaceDTO();
        space.setUserId(userId);
        space.setName(name);
        return space;
    }

    @Test
    void insertSpace_thenFindById() {
        SpaceDTO space = buildSpace("창고");
        spaceMapper.insertSpace(space);

        Optional<SpaceDTO> found = spaceMapper.findById(space.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getExternalId()).isNotNull();
        assertThat(found.get().getName()).isEqualTo("창고");
        assertThat(found.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void findById_notFound_returnsEmpty() {
        Optional<SpaceDTO> found = spaceMapper.findById(999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findByUserId() {
        spaceMapper.insertSpace(buildSpace("창고A"));
        spaceMapper.insertSpace(buildSpace("창고B"));

        List<SpaceDTO> spaces = spaceMapper.findByUserId(userId);

        assertThat(spaces).hasSize(2);
        assertThat(spaces).extracting(SpaceDTO::getName).containsExactlyInAnyOrder("창고A", "창고B");
    }

    @Test
    void findByUserIdWithOptions_filtersByNameAndSorts() {
        spaceMapper.insertSpace(buildSpace("창고B"));
        spaceMapper.insertSpace(buildSpace("창고A"));
        spaceMapper.insertSpace(buildSpace("매장"));

        List<SpaceDTO> searched = spaceMapper.findByUserIdWithOptions(userId, "창고", "name", 10, 0);
        List<SpaceDTO> newest = spaceMapper.findByUserIdWithOptions(userId, null, "newest", 10, 0);

        assertThat(searched).extracting(SpaceDTO::getName).containsExactly("창고A", "창고B");
        assertThat(newest).extracting(SpaceDTO::getName).containsExactly("매장", "창고A", "창고B");
        assertThat(spaceMapper.countByUserIdWithOptions(userId, "창고")).isEqualTo(2);
    }

    @Test
    void findByUserIdWithOptions_appliesLimitAndOffset() {
        for (int i = 0; i < 12; i++) {
            spaceMapper.insertSpace(buildSpace("공간%02d".formatted(i)));
        }

        List<SpaceDTO> firstPage = spaceMapper.findByUserIdWithOptions(userId, null, "name", 10, 0);
        List<SpaceDTO> secondPage = spaceMapper.findByUserIdWithOptions(userId, null, "name", 10, 10);

        assertThat(firstPage).hasSize(10);
        assertThat(secondPage).extracting(SpaceDTO::getName).containsExactly("공간10", "공간11");
        assertThat(spaceMapper.countByUserIdWithOptions(userId, null)).isEqualTo(12);
    }

    @Test
    void updateSpace() {
        SpaceDTO space = buildSpace("구창고");
        spaceMapper.insertSpace(space);
        space.setName("신창고");

        spaceMapper.updateSpace(space);

        Optional<SpaceDTO> found = spaceMapper.findById(space.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("신창고");
    }

    @Test
    void deleteById() {
        SpaceDTO space = buildSpace("삭제창고");
        spaceMapper.insertSpace(space);

        spaceMapper.deleteById(space.getId());

        assertThat(spaceMapper.findById(space.getId())).isEmpty();
    }
}
