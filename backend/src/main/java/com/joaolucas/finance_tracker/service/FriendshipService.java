package com.joaolucas.finance_tracker.service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.joaolucas.finance_tracker.dto.friend.FriendDTO;
import com.joaolucas.finance_tracker.dto.friend.FriendRequestDTO;
import com.joaolucas.finance_tracker.dto.friend.FriendRequestItemDTO;
import com.joaolucas.finance_tracker.dto.friend.FriendRequestResponseDTO;
import com.joaolucas.finance_tracker.entity.Friendship;
import com.joaolucas.finance_tracker.entity.FriendshipStatus;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.entity.UserProfile;
import com.joaolucas.finance_tracker.exception.ConflictException;
import com.joaolucas.finance_tracker.exception.ForbiddenException;
import com.joaolucas.finance_tracker.exception.NotFoundException;
import com.joaolucas.finance_tracker.mapper.FriendshipMapper;
import com.joaolucas.finance_tracker.repository.FriendshipRepository;
import com.joaolucas.finance_tracker.repository.UserProfileRepository;
import com.joaolucas.finance_tracker.repository.UserRepository;


@Service
public class FriendshipService {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendshipMapper friendshipMapper;

    public FriendshipService(AuthService authService, UserRepository userRepository,
            UserProfileRepository userProfileRepository, FriendshipRepository friendshipRepository,
            FriendshipMapper friendshipMapper) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendshipMapper = friendshipMapper;
    }

    public List<FriendDTO> getFriends() {
        User user = this.authService.getAuthenticatedUser();
        Long userId = user.getId();

        List<User> friends = this.friendshipRepository
                .findAllByUserAndStatus(userId, FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> otherUser(friendship, userId))
                .toList();

        Map<Long, UserProfile> profiles = profilesByUserId(friends);

        return friends.stream()
                .map(friend -> this.friendshipMapper.toFriendDTO(friend, profiles.get(friend.getId())))
                .toList();
    }

    public FriendRequestResponseDTO sendRequest(FriendRequestDTO requestDTO) {

        User requester = this.authService.getAuthenticatedUser();

        User addressee = this.userRepository.findByEmail(requestDTO.getEmail().trim())
                .orElseThrow(() -> new NotFoundException("Nenhum usuário encontrado com este e-mail"));

        if (addressee.getId().equals(requester.getId())) {
            throw new ConflictException("Você não pode adicionar a si mesmo como amigo");
        }

        this.friendshipRepository.findBetween(requester.getId(), addressee.getId())
                .ifPresent(existing -> {
                    if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                        throw new ConflictException("Vocês já são amigos");
                    }
                    throw new ConflictException("Já existe um convite pendente entre vocês");
                });

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status(FriendshipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Friendship saved = this.friendshipRepository.save(friendship);

        return this.friendshipMapper.toRequestResponse(saved);
    }

    public List<FriendRequestItemDTO> getIncomingRequests() {
        User user = this.authService.getAuthenticatedUser();

        List<Friendship> requests = this.friendshipRepository
                .findAllByAddresseeAndStatus(user.getId(), FriendshipStatus.PENDING);

        Map<Long, UserProfile> profiles = profilesByUserId(
                requests.stream().map(Friendship::getRequester).toList());

        return requests.stream()
                .map(friendship -> this.friendshipMapper.toRequestItem(
                        friendship, profiles.get(friendship.getRequester().getId())))
                .toList();
    }

    public FriendDTO acceptRequest(Long requestId) {
        User user = this.authService.getAuthenticatedUser();
        Friendship friendship = getPendingRequestForAddressee(requestId, user);

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setUpdatedAt(LocalDateTime.now());

        Friendship saved = this.friendshipRepository.save(friendship);

        User friend = saved.getRequester();
        UserProfile profile = this.userProfileRepository.findByUserId(friend.getId()).orElse(null);

        return this.friendshipMapper.toFriendDTO(friend, profile);
    }

    public void rejectRequest(Long requestId) {
        User user = this.authService.getAuthenticatedUser();
        Friendship friendship = getPendingRequestForAddressee(requestId, user);

        this.friendshipRepository.delete(friendship);
    }

    private Friendship getPendingRequestForAddressee(Long requestId, User user) {
        Friendship friendship = this.friendshipRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Convite não encontrado"));

        if (!friendship.getAddressee().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não pode responder a este convite");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ConflictException("Este convite não está mais pendente");
        }

        return friendship;
    }

    private User otherUser(Friendship friendship, Long currentUserId) {
        return friendship.getRequester().getId().equals(currentUserId)
                ? friendship.getAddressee()
                : friendship.getRequester();
    }

    private Map<Long, UserProfile> profilesByUserId(List<User> users) {
        List<Long> ids = users.stream().map(User::getId).toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        return this.userProfileRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), profile -> profile));
    }
}
