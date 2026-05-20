package com.seu.seustock.model.form;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
public class QuickStockForm {

    @NotBlank(message = "품목명을 입력해주세요.")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull(message = "공간 정보가 없습니다.")
    private UUID spaceExternalId;
    private UUID shelfExternalId;
    private UUID boxExternalId;
    @Min(1) @Max(50)
    private int count = 1;

    private String memo;

    private MultipartFile imageFile;
    private String imageHash;
}
