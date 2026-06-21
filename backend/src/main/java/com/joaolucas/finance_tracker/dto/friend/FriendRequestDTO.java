package com.joaolucas.finance_tracker.dto.friend;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class FriendRequestDTO {

    @NotBlank (message = "E-mail é obrigatório")
    @Email (message = "E-mail inválido")
    private String email;
}
