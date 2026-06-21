package com.joaolucas.finance_tracker.dto.friend;


import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class FriendRequestItemDTO {
    private Long id;
    private String name;
    private String email;
    private String photoBase64;
    private String photoType;
}
