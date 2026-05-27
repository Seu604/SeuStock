package com.seu.seustock.service;

import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.UserDTO;
import com.seu.seustock.model.form.LoginForm;
import com.seu.seustock.model.form.UserRegistrationForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String RAW_PASSWORD = "plaintext123";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedHashValue";

    @Mock private UserMapper       userMapper;
    @Mock private PasswordEncoder  passwordEncoder;

    @InjectMocks private UserService userService;

    // ── existsByUsername ──────────────────────────────────────────────────────

    @Test
    void existsByUsername_returnsTrueWhenPresent() {
        UserDTO existing = new UserDTO();
        existing.setUsername("alice");

        when(userMapper.findByUsername("alice")).thenReturn(Optional.of(existing));

        assertThat(userService.existsByUsername("alice")).isTrue();
    }

    @Test
    void existsByUsername_returnsFalseWhenAbsent() {
        when(userMapper.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThat(userService.existsByUsername("ghost")).isFalse();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_encodesPasswordBeforeInserting() {
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        UserRegistrationForm form = new UserRegistrationForm();
        form.setUsername("newuser");
        form.setPassword(RAW_PASSWORD);

        userService.register(form);

        ArgumentCaptor<UserDTO> captor = ArgumentCaptor.forClass(UserDTO.class);
        verify(userMapper).insertUser(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(captor.getValue().getUsername()).isEqualTo("newuser");
    }

    @Test
    void register_neverStoresRawPassword() {
        when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_PASSWORD);

        UserRegistrationForm form = new UserRegistrationForm();
        form.setUsername("newuser");
        form.setPassword(RAW_PASSWORD);

        userService.register(form);

        ArgumentCaptor<UserDTO> captor = ArgumentCaptor.forClass(UserDTO.class);
        verify(userMapper).insertUser(captor.capture());
        assertThat(captor.getValue().getPassword()).isNotEqualTo(RAW_PASSWORD);
    }

    // ── authenticate ──────────────────────────────────────────────────────────

    @Test
    void authenticate_returnsUserOnCorrectCredentials() {
        UserDTO stored = new UserDTO();
        stored.setUsername("alice");
        stored.setPassword(ENCODED_PASSWORD);

        when(userMapper.findByUsername("alice")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        LoginForm form = new LoginForm();
        form.setUsername("alice");
        form.setPassword(RAW_PASSWORD);

        Optional<UserDTO> result = userService.authenticate(form);

        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(stored);
    }

    @Test
    void authenticate_returnsEmptyOnWrongPassword() {
        UserDTO stored = new UserDTO();
        stored.setUsername("alice");
        stored.setPassword(ENCODED_PASSWORD);

        when(userMapper.findByUsername("alice")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches(eq("wrongpassword"), anyString())).thenReturn(false);

        LoginForm form = new LoginForm();
        form.setUsername("alice");
        form.setPassword("wrongpassword");

        Optional<UserDTO> result = userService.authenticate(form);

        assertThat(result).isEmpty();
    }

    @Test
    void authenticate_returnsEmptyWhenUserNotFound() {
        when(userMapper.findByUsername("unknown")).thenReturn(Optional.empty());

        LoginForm form = new LoginForm();
        form.setUsername("unknown");
        form.setPassword(RAW_PASSWORD);

        Optional<UserDTO> result = userService.authenticate(form);

        assertThat(result).isEmpty();
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
