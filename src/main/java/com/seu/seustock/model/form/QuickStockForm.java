package com.seu.seustock.model.form;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
public class QuickStockForm {

    @NotBlank(message = "{valid.quickStock.name.notBlank}")
    @Size(max = 100, message = "{valid.quickStock.name.size}")
    private String name;

    @Size(max = 500, message = "{valid.quickStock.description.size}")
    private String description;

    @NotNull(message = "{valid.quickStock.spaceExternalId.notNull}")
    private UUID spaceExternalId;
    private UUID shelfExternalId;
    private UUID boxExternalId;
    @Min(value = 1, message = "{valid.quickStock.count.min}") 
    @Max(value = 50, message = "{valid.quickStock.count.max}")
    private int count = 1;

    private String memo;

    private MultipartFile imageFile;
    private String imageHash;
}
