package com.joaolucas.finance_tracker.dto.friend;


import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class FriendRequestResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String status;
}
