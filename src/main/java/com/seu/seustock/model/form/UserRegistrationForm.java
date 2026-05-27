package com.seu.seustock.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationForm {

    @NotBlank(message = "{valid.userRegistration.username.notBlank}")
    @Size(min = 4, max = 20, message = "{valid.userRegistration.username.size}")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "{valid.userRegistration.username.pattern}")
    private String username;

    @NotBlank(message = "{valid.userRegistration.password.notBlank}")
    @Size(min = 8, max = 50, message = "{valid.userRegistration.password.size}")
    private String password;

    @NotBlank(message = "{valid.userRegistration.passwordConfirm.notBlank}")
    private String passwordConfirm;
}
