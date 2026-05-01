package dev.ceven.petapp.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import dev.ceven.petapp.config.SecurityConfig;
import dev.ceven.petapp.controller.PetController;
import dev.ceven.petapp.dto.PetDto;
import dev.ceven.petapp.service.PetService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.context.annotation.Import;

@WebMvcTest(PetController.class)
@Import(SecurityConfig.class)
class PetControllerTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetService petService;

    @Test
    void statusEndpointShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/public/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("Pet App is running!"));
    }

    @Test
    @WithMockUser(roles = "admin")
    void createPetShouldRequireAdminRoleAndProduceEvent() throws Exception {
        PetDto mockResponse = new PetDto("1", "Buddy", "Dog");

        Mockito.when(petService.createPet(any(PetDto.class))).thenReturn(mockResponse);

        String petJson = "{\"name\":\"Buddy\",\"species\":\"Dog\"}";

        mockMvc.perform(post("/api/pets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(petJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Buddy"));

        Mockito.verify(petService, Mockito.times(1)).createPet(any());
    }

    @Test
    @WithMockUser(roles = "user")
    void getAllPetsShouldRequireUserRole() throws Exception {
        PetDto mockDto = new PetDto("1", "Buddy", "Dog");
        Mockito.when(petService.getAllPets()).thenReturn(List.of(mockDto));

        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Buddy"));
    }

    @Test
    @WithMockUser(roles = "user")
    void searchPetsShouldRequireUserRole() throws Exception {
        PetDto mockDto = new PetDto("1", "Buddy", "Dog");
        Mockito.when(petService.searchPets("Buddy")).thenReturn(List.of(mockDto));

        mockMvc.perform(get("/api/pets/search").param("name", "Buddy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Buddy"));
    }
}
