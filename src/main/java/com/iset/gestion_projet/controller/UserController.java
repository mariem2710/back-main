package com.iset.gestion_projet.controller;

import com.iset.gestion_projet.DTOS.LoginResponse;
import com.iset.gestion_projet.DTOS.UserResponse;
import com.iset.gestion_projet.Request.UserRequest;
import com.iset.gestion_projet.entity.Role;
import com.iset.gestion_projet.service.JwtService;
import com.iset.gestion_projet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    // ─────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserRequest request) {
        try {
            var user = userService.login(request.getEmail(), request.getPassword());
            String token = jwtService.generateToken(user.getEmail());
            return ResponseEntity.ok(new LoginResponse(
                    token,
                    user.getEmail(),
                    user.getRole().name(),
                    user.getId(),
                    user.getNom(),
                    user.getPrenom()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // ACCOUNT REQUESTS
    // ─────────────────────────────────────────────

    @PostMapping("/demande")
    public ResponseEntity<?> demanderCompte(@RequestBody UserRequest request) {
        try {
            return ResponseEntity.ok(userService.demanderCompte(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/demandes/en-attente")
    public ResponseEntity<List<UserResponse>> getDemandesEnAttente() {
        return ResponseEntity.ok(userService.getDemandesEnAttente());
    }

    @PutMapping("/{id}/accepter")
    public ResponseEntity<?> accepterCompte(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le mot de passe est obligatoire."));
        }
        try {
            return ResponseEntity.ok(userService.accepterCompte(id, password));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/refuser")
    public ResponseEntity<?> refuserCompte(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.refuserCompte(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // ADMIN: CREATE ACCOUNT DIRECTLY
    // ─────────────────────────────────────────────

    // POST /api/users/creer  — admin creates account with password + equipe
    @PostMapping("/creer")
    public ResponseEntity<?> creerCompte(@RequestBody UserRequest request) {
        try {
            return ResponseEntity.ok(userService.creerCompte(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // PUT /api/users/{userId}/equipe/{equipeId} — admin assigns user to equipe
    @PutMapping("/{userId}/equipe/{equipeId}")
    public ResponseEntity<?> assignerEquipe(
            @PathVariable Long userId,
            @PathVariable Long equipeId) {
        try {
            return ResponseEntity.ok(userService.assignerEquipe(userId, equipeId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponse>> getByRole(@PathVariable Role role) {
        return ResponseEntity.ok(userService.getByRole(role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.getUserById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody UserRequest request) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // DEV UTILITY (remove before production)
    // ─────────────────────────────────────────────

    @GetMapping("/hash")
    public String hash(@RequestParam String p) {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(p);
    }
}