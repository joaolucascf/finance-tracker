package com.joaolucas.finance_tracker.mapper;


import java.util.Base64;

import org.springframework.stereotype.Component;

import com.joaolucas.finance_tracker.dto.friend.FriendDTO;
import com.joaolucas.finance_tracker.dto.friend.FriendRequestItemDTO;
import com.joaolucas.finance_tracker.dto.friend.FriendRequestResponseDTO;
import com.joaolucas.finance_tracker.entity.Friendship;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.entity.UserProfile;

import jakarta.annotation.Nullable;


@Component
public class FriendshipMapper {

    public FriendDTO toFriendDTO(User friend, @Nullable UserProfile profile) {
        return FriendDTO.builder()
                .id(friend.getId())
                .name(displayName(friend, profile))
                .email(friend.getEmail())
                .photoBase64(encodePhoto(profile))
                .photoType(profile != null ? profile.getPhotoType() : null)
                .build();
    }

    public FriendRequestItemDTO toRequestItem(Friendship friendship, @Nullable UserProfile profile) {
        User requester = friendship.getRequester();

        return FriendRequestItemDTO.builder()
                .id(friendship.getId())
                .name(displayName(requester, profile))
                .email(requester.getEmail())
                .photoBase64(encodePhoto(profile))
                .photoType(profile != null ? profile.getPhotoType() : null)
                .build();
    }

    public FriendRequestResponseDTO toRequestResponse(Friendship friendship) {
        User addressee = friendship.getAddressee();

        return FriendRequestResponseDTO.builder()
                .id(friendship.getId())
                .name(addressee.getName())
                .email(addressee.getEmail())
                .status(friendship.getStatus().name())
                .build();
    }

    private String displayName(User user, @Nullable UserProfile profile) {
        if (profile != null && profile.getNickname() != null && !profile.getNickname().isBlank()) {
            return profile.getNickname();
        }
        return user.getName();
    }

    @Nullable
    private String encodePhoto(@Nullable UserProfile profile) {
        if (profile == null || profile.getPhoto() == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(profile.getPhoto());
    }
}
