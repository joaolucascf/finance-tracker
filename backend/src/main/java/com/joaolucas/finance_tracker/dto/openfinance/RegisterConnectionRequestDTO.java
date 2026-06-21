package com.joaolucas.finance_tracker.dto.openfinance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterConnectionRequestDTO {

    @NotBlank
    private String itemId;

    @NotBlank
    private String institutionName;
}
