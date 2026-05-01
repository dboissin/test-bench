package dev.ceven.petapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import dev.ceven.petapp.document.PetDocument;
import dev.ceven.petapp.dto.PetEvent;
import dev.ceven.petapp.repository.PetSearchRepository;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    private final PetSearchRepository petSearchRepository;

    public KafkaConsumerService(PetSearchRepository petSearchRepository) {
        this.petSearchRepository = petSearchRepository;
    }

    @KafkaListener(topics = "petapp-events", groupId = "petapp-group")
    public void consumePetEvent(PetEvent event) {
        logger.info("Consumed pet event for: {}", event.getName());
        PetDocument document = new PetDocument(event.getId(), event.getName(), event.getSpecies());
        petSearchRepository.save(document);
        logger.info("Indexed pet in Elasticsearch: {}", event.getId());
    }
}
