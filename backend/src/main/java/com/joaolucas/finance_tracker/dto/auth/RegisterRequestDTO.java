package com.joaolucas.finance_tracker.dto.auth;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class RegisterRequestDTO {

    @NotBlank
    private String name;
    @NotBlank
    private String email;
    @NotBlank
    private String password;
}
