package com.iset.gestion_projet.controller;

import com.iset.gestion_projet.DTOS.MembreDTO;
import com.iset.gestion_projet.DTOS.MembreRequest;
import com.iset.gestion_projet.service.MembreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membres")
@RequiredArgsConstructor
public class MembreController {

    private final MembreService membreService;

    @GetMapping
    public ResponseEntity<List<MembreDTO>> getAll() {
        return ResponseEntity.ok(membreService.getAll());
    }

    @PostMapping
    public ResponseEntity<MembreDTO> create(@RequestBody MembreRequest req) {
        return ResponseEntity.ok(membreService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MembreDTO> update(
            @PathVariable Long id,
            @RequestBody MembreRequest req) {
        return ResponseEntity.ok(membreService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        membreService.delete(id);
        return ResponseEntity.noContent().build();
    }
}