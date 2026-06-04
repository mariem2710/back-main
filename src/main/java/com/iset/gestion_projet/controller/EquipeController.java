package com.iset.gestion_projet.controller;

import com.iset.gestion_projet.DTOS.EquipeDTO;
import com.iset.gestion_projet.DTOS.MembreDTO;
import com.iset.gestion_projet.service.EquipeService;
import com.iset.gestion_projet.service.MembreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipes")
@RequiredArgsConstructor
public class EquipeController {

    private final EquipeService equipeService;
    private final MembreService membreService;

    @GetMapping
    public ResponseEntity<List<EquipeDTO>> getAll() {
        return ResponseEntity.ok(equipeService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipeDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(equipeService.getById(id));
    }

    @GetMapping("/{id}/membres")
    public ResponseEntity<List<MembreDTO>> getMembres(@PathVariable Long id) {
        return ResponseEntity.ok(membreService.getByEquipe(id));
    }

    @PostMapping
    public ResponseEntity<EquipeDTO> create(@RequestBody EquipeDTO dto) {
        return ResponseEntity.ok(equipeService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipeDTO> update(
            @PathVariable Long id,
            @RequestBody EquipeDTO dto) {
        return ResponseEntity.ok(equipeService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}