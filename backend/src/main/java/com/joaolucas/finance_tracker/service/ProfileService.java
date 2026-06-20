package com.joaolucas.finance_tracker.service;

import com.joaolucas.finance_tracker.dto.profile.ProfileRequestDTO;
import com.joaolucas.finance_tracker.dto.profile.ProfileResponseDTO;
import com.joaolucas.finance_tracker.entity.User;
import com.joaolucas.finance_tracker.entity.UserProfile;
import com.joaolucas.finance_tracker.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Service
public class ProfileService {

    private final AuthService authService;
    private final UserProfileRepository userProfileRepository;

    public ProfileService(AuthService authService, UserProfileRepository userProfileRepository) {
        this.authService = authService;
        this.userProfileRepository = userProfileRepository;
    }

    public ProfileResponseDTO getProfile() {
        User user = authService.getAuthenticatedUser();
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(UserProfile::new);
        return toResponse(user, profile);
    }

    public ProfileResponseDTO updateProfile(ProfileRequestDTO dto) {
        User user = authService.getAuthenticatedUser();

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().user(user).build());

        profile.setNickname(dto.getNickname());
        profile.setBirthDate(dto.getBirthDate());
        profile.setMonthlyIncome(dto.getMonthlyIncome());
        profile.setMaritalStatus(dto.getMaritalStatus());

        userProfileRepository.save(profile);
        return toResponse(user, profile);
    }

    public ProfileResponseDTO uploadPhoto(MultipartFile file) {
        User user = authService.getAuthenticatedUser();

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().user(user).build());

        try {
            profile.setPhoto(file.getBytes());
            profile.setPhotoType(file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar imagem", e);
        }

        userProfileRepository.save(profile);
        return toResponse(user, profile);
    }

    private ProfileResponseDTO toResponse(User user, UserProfile profile) {
        String photoBase64 = null;
        if (profile.getPhoto() != null) {
            photoBase64 = Base64.getEncoder().encodeToString(profile.getPhoto());
        }

        return ProfileResponseDTO.builder()
                .name(user.getName())
                .email(user.getEmail())
                .nickname(profile.getNickname())
                .birthDate(profile.getBirthDate())
                .monthlyIncome(profile.getMonthlyIncome())
                .maritalStatus(profile.getMaritalStatus())
                .photoBase64(photoBase64)
                .photoType(profile.getPhotoType())
                .build();
    }
}
