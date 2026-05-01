package dev.ceven.petapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dev.ceven.petapp.dto.PetDto;
import dev.ceven.petapp.service.PetService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping("/public/status")
    public String status() {
        return "Pet App is running!";
    }

    @PostMapping("/pets")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<PetDto> createPet(@RequestBody PetDto petDto) {
        PetDto responseDto = petService.createPet(petDto);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/pets")
    @PreAuthorize("hasRole('user')")
    public List<PetDto> getAllPets() {
        return petService.getAllPets();
    }

    @GetMapping("/pets/search")
    @PreAuthorize("hasRole('user')")
    public List<PetDto> searchPets(@RequestParam String name) {
        return petService.searchPets(name);
    }
}
