package com.joaolucas.finance_tracker.dto.category;


import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class CategoryResponseDTO {
    private Long id;
    private String name;
}
