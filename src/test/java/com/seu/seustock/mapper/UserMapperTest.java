package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("classpath:schema-test.sql")
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    private UserDTO buildUser(String username) {
        UserDTO user = new UserDTO();
        user.setUsername(username);
        user.setPassword("password");
        return user;
    }

    @Test
    void insertUser_thenFindByUsername() {
        UserDTO user = buildUser("alice");
        userMapper.insertUser(user);

        Optional<UserDTO> found = userMapper.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getExternalId()).isNotNull();
        assertThat(found.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void findByUsername_notFound_returnsEmpty() {
        Optional<UserDTO> found = userMapper.findByUsername("nobody");
        assertThat(found).isEmpty();
    }

    @Test
    void updatePassword() {
        UserDTO user = buildUser("bob");
        userMapper.insertUser(user);
        user.setPassword("newPassword");

        userMapper.updatePassword(user);

        Optional<UserDTO> found = userMapper.findByUsername("bob");
        assertThat(found).isPresent();
        assertThat(found.get().getPassword()).isEqualTo("newPassword");
    }

    @Test
    void deleteById() {
        UserDTO user = buildUser("charlie");
        userMapper.insertUser(user);

        userMapper.deleteById(user.getId());

        assertThat(userMapper.findByUsername("charlie")).isEmpty();
    }
}
