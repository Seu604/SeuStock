package com.seu.seustock.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoxForm {

    @NotBlank(message = "박스 이름을 입력해주세요.")
    @Size(max = 100, message = "박스 이름은 100자 이하여야 합니다.")
    private String name;
}
