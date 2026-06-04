package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.profile.ChangePasswordDTO;
import com.iset.gestion_projet.DTOS.profile.ProfileResponseDTO;
import com.iset.gestion_projet.DTOS.profile.ProfileUpdateDTO;
import com.iset.gestion_projet.entity.User;
import com.iset.gestion_projet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ProfileResponseDTO getProfile(String authorizationHeader) {
        String email = jwtService.extractEmailFromToken(authorizationHeader);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return mapToProfileResponse(user);
    }

    @Transactional
    public ProfileResponseDTO updateProfile(String authorizationHeader, ProfileUpdateDTO dto) {
        String email = jwtService.extractEmailFromToken(authorizationHeader);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (dto.getNom() != null) user.setNom(dto.getNom());
        if (dto.getPrenom() != null) user.setPrenom(dto.getPrenom());
        if (dto.getTelephone() != null) user.setTelephone(dto.getTelephone());
        if (dto.getPoste() != null) user.setPoste(dto.getPoste());
        if (dto.getDateNaissance() != null) user.setDateNaissance(dto.getDateNaissance());

        user = userRepository.save(user);
        return mapToProfileResponse(user);
    }

    @Transactional
    public void changePassword(String authorizationHeader, ChangePasswordDTO dto) {
        String email = jwtService.extractEmailFromToken(authorizationHeader);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 6) {
            throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 6 caractères");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    private ProfileResponseDTO mapToProfileResponse(User user) {
        return ProfileResponseDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .poste(user.getPoste())
                .avatarUrl(user.getAvatarUrl())
                .dateNaissance(user.getDateNaissance())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .equipe(user.getEquipe() != null ? user.getEquipe().getNom() : null)
                .build();
    }
}