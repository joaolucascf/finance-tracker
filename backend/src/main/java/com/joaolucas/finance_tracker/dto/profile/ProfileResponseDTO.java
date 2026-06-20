package com.joaolucas.finance_tracker.dto.profile;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ProfileResponseDTO {

    private String name;
    private String email;
    private String nickname;
    private LocalDate birthDate;
    private BigDecimal monthlyIncome;
    private String maritalStatus;
    private String photoBase64;
    private String photoType;
}
