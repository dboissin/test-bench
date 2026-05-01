package dev.ceven.petapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import dev.ceven.petapp.dto.PetEvent;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "petapp-events";

    private final KafkaTemplate<String, PetEvent> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, PetEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPetEvent(PetEvent event) {
        logger.info("Producing pet event: {}", event.getName());
        kafkaTemplate.send(TOPIC, event.getId(), event);
    }
}
