package dev.ceven.petapp.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import dev.ceven.petapp.document.PetDocument;

import java.util.List;

@Repository
public interface PetSearchRepository extends ElasticsearchRepository<PetDocument, String> {
    List<PetDocument> findByName(String name);
}
