package dev.ceven.petapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.ceven.petapp.document.PetDocument;
import dev.ceven.petapp.domain.Pet;
import dev.ceven.petapp.dto.PetDto;
import dev.ceven.petapp.repository.PetRepository;
import dev.ceven.petapp.repository.PetSearchRepository;
import dev.ceven.petapp.service.KafkaProducerService;
import dev.ceven.petapp.service.PetService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetSearchRepository petSearchRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private PetService petService;

    @Test
    void testCreatePet() {
        PetDto inputDto = new PetDto(null, "Buddy", "Dog");
        Pet mockPet = new Pet("Buddy", "Dog");
        mockPet.setId(1L);

        Mockito.when(petRepository.save(any(Pet.class))).thenReturn(mockPet);

        PetDto result = petService.createPet(inputDto);

        assertNotNull(result);
        assertEquals("1", result.getId());
        assertEquals("Buddy", result.getName());
        assertEquals("Dog", result.getSpecies());

        Mockito.verify(petRepository, Mockito.times(1)).save(any(Pet.class));
        Mockito.verify(kafkaProducerService, Mockito.times(1)).sendPetEvent(any());
    }

    @Test
    void testGetAllPets() {
        Pet mockPet = new Pet("Buddy", "Dog");
        mockPet.setId(1L);
        Mockito.when(petRepository.findAll()).thenReturn(List.of(mockPet));

        List<PetDto> result = petService.getAllPets();

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("Buddy", result.get(0).getName());
        Mockito.verify(petRepository, Mockito.times(1)).findAll();
    }

    @Test
    void testSearchPets() {
        PetDocument mockDoc = new PetDocument("1", "Buddy", "Dog");
        Mockito.when(petSearchRepository.findByName("Buddy")).thenReturn(List.of(mockDoc));

        List<PetDto> result = petService.searchPets("Buddy");

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("Buddy", result.get(0).getName());
        Mockito.verify(petSearchRepository, Mockito.times(1)).findByName("Buddy");
    }
}
