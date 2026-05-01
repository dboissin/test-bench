package dev.ceven.petapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.ceven.petapp.domain.Pet;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
}
