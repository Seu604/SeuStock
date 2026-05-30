package com.seu.seustock.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
public class UserDTO {
    private Long id;
    private UUID externalId;
    private String email;
    private String nickname;
    private String password;
    private LocalDateTime createdAt;
}
