package com.joaolucas.finance_tracker.dto.profile;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProfileRequestDTO {

    private String nickname;

    private LocalDate birthDate;

    private BigDecimal monthlyIncome;

    private String maritalStatus;
}
