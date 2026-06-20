package com.joaolucas.finance_tracker.controller;

import com.joaolucas.finance_tracker.dto.profile.ProfileRequestDTO;
import com.joaolucas.finance_tracker.dto.profile.ProfileResponseDTO;
import com.joaolucas.finance_tracker.service.ProfileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponseDTO getProfile() {
        return profileService.getProfile();
    }

    @PatchMapping("/me")
    public ProfileResponseDTO updateProfile(@RequestBody ProfileRequestDTO dto) {
        return profileService.updateProfile(dto);
    }

    @PostMapping("/me/photo")
    public ProfileResponseDTO uploadPhoto(@RequestParam("photo") MultipartFile photo) {
        return profileService.uploadPhoto(photo);
    }
}
