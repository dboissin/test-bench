package dev.ceven.petapp.repository;

import dev.ceven.petapp.entity.ProcessingMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcessingMessageRepository extends JpaRepository<ProcessingMessage, Long> {
    List<ProcessingMessage> findByStatus(String status);
}
