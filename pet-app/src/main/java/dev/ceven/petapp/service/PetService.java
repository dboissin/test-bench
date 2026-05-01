package dev.ceven.petapp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.ceven.petapp.domain.Pet;
import dev.ceven.petapp.dto.PetDto;
import dev.ceven.petapp.dto.PetEvent;
import dev.ceven.petapp.repository.PetRepository;
import dev.ceven.petapp.repository.PetSearchRepository;

import java.util.List;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final PetSearchRepository petSearchRepository;
    private final KafkaProducerService kafkaProducerService;

    public PetService(PetRepository petRepository, PetSearchRepository petSearchRepository,
            KafkaProducerService kafkaProducerService) {
        this.petRepository = petRepository;
        this.petSearchRepository = petSearchRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Transactional
    public PetDto createPet(PetDto petDto) {
        Pet pet = new Pet(petDto.getName(), petDto.getSpecies());
        Pet savedPet = petRepository.save(pet);

        PetEvent event = new PetEvent(savedPet.getId().toString(), savedPet.getName(), savedPet.getSpecies());
        kafkaProducerService.sendPetEvent(event);

        return new PetDto(savedPet.getId().toString(), savedPet.getName(), savedPet.getSpecies());
    }

    public List<PetDto> getAllPets() {
        return petRepository.findAll().stream()
                .map(pet -> new PetDto(pet.getId().toString(), pet.getName(), pet.getSpecies()))
                .toList();
    }

    public List<PetDto> searchPets(String name) {
        return petSearchRepository.findByName(name).stream()
                .map(doc -> new PetDto(doc.getId(), doc.getName(), doc.getSpecies()))
                .toList();
    }

}
