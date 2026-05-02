package dev.ceven.petapp.controller;

import dev.ceven.petapp.entity.ProcessingMessage;
import dev.ceven.petapp.service.ProcessingMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ProcessingMessageService processingMessageService;

    public MessageController(ProcessingMessageService processingMessageService) {
        this.processingMessageService = processingMessageService;
    }

    @GetMapping
    @PreAuthorize("hasRole('user')")
    public ResponseEntity<List<ProcessingMessage>> getMessages(@RequestParam String threadId) {
        List<ProcessingMessage> messages = processingMessageService.fetchMessages(threadId);
        return ResponseEntity.ok(messages);
    }
}
