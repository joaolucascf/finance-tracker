package com.joaolucas.finance_tracker.controller;


import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joaolucas.finance_tracker.dto.friend.FriendDTO;
import com.joaolucas.finance_tracker.dto.friend.FriendRequestDTO;
import com.joaolucas.finance_tracker.dto.friend.FriendRequestItemDTO;
import com.joaolucas.finance_tracker.dto.friend.FriendRequestResponseDTO;
import com.joaolucas.finance_tracker.service.FriendshipService;

import jakarta.validation.Valid;


@RestController
@RequestMapping ("/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping
    public List<FriendDTO> getFriends() {
        return this.friendshipService.getFriends();
    }

    @PostMapping ("/requests")
    public FriendRequestResponseDTO sendRequest(@RequestBody @Valid FriendRequestDTO request) {
        return this.friendshipService.sendRequest(request);
    }

    @GetMapping ("/requests")
    public List<FriendRequestItemDTO> getIncomingRequests() {
        return this.friendshipService.getIncomingRequests();
    }

    @PostMapping ("/requests/{id}/accept")
    public FriendDTO acceptRequest(@PathVariable Long id) {
        return this.friendshipService.acceptRequest(id);
    }

    @PostMapping ("/requests/{id}/reject")
    public void rejectRequest(@PathVariable Long id) {
        this.friendshipService.rejectRequest(id);
    }
}
