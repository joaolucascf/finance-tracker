package com.joaolucas.finance_tracker.dto.bill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BillRenameRequestDTO {

    @NotBlank
    @Size(max = 60)
    private String name;
}
