package com.seu.seustock.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {

    @NotBlank(message = "{valid.login.username.notBlank}")
    private String username;

    @NotBlank(message = "{valid.login.password.notBlank}")
    private String password;
}
