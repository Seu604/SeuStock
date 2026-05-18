package com.seu.seustock.service;

import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.UserDTO;
import com.seu.seustock.model.form.LoginForm;
import com.seu.seustock.model.form.UserRegistrationForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public boolean existsByUsername(String username) {
        return userMapper.findByUsername(username).isPresent();
    }

    public void register(UserRegistrationForm form) {
        UserDTO user = new UserDTO();
        user.setUsername(form.getUsername());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        userMapper.insertUser(user);
    }

    public Optional<UserDTO> authenticate(LoginForm form) {
        return userMapper.findByUsername(form.getUsername())
                .filter(user -> passwordEncoder.matches(form.getPassword(), user.getPassword()));
    }
}
