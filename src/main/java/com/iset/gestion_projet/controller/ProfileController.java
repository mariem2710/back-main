package com.iset.gestion_projet.controller;

import com.iset.gestion_projet.DTOS.profile.ChangePasswordDTO;
import com.iset.gestion_projet.DTOS.profile.ProfileResponseDTO;
import com.iset.gestion_projet.DTOS.profile.ProfileUpdateDTO;
import com.iset.gestion_projet.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponseDTO> getProfile(
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(profileService.getProfile(authorizationHeader));
    }

    @PutMapping
    public ResponseEntity<ProfileResponseDTO> updateProfile(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody ProfileUpdateDTO dto) {
        return ResponseEntity.ok(profileService.updateProfile(authorizationHeader, dto));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody ChangePasswordDTO dto) {
        profileService.changePassword(authorizationHeader, dto);
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
    }
}