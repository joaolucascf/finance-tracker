package com.joaolucas.finance_tracker.dto.category;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class CategoryRequestDTO {

    @NotBlank
    private String name;
}
