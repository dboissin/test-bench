package dev.ceven.petapp.service;

import dev.ceven.petapp.entity.ProcessingMessage;

import org.infinispan.lock.api.ClusteredLock;
import org.infinispan.lock.api.ClusteredLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MessageWorker {

    private static final Logger log = LoggerFactory.getLogger(MessageWorker.class);

    private final ProcessingMessageService messageService;
    private final SoapMessageClient soapClient;
    private final ClusteredLockManager lockManager;

    public MessageWorker(ProcessingMessageService messageService, SoapMessageClient soapClient,
            ClusteredLockManager lockManager) {
        this.messageService = messageService;
        this.soapClient = soapClient;
        this.lockManager = lockManager;
    }

    @Scheduled(fixedDelay = 5000)
    public void processMessages() {
        List<ProcessingMessage> pendingMessages = messageService.getPendingMessages();

        if (pendingMessages.isEmpty()) {
            return;
        }

        for (ProcessingMessage msg : pendingMessages) {
            String lockKey = "lock:thread:" + msg.getThreadId();

            // Define lock in the cluster if it doesn't exist
            lockManager.defineLock(lockKey);
            ClusteredLock lock = lockManager.get(lockKey);

            boolean acquired = false;
            try {
                // Try to acquire distributed lock for this threadId
                // We use tryLock with a timeout to simulate the lifespan so it doesn't block
                // forever
                // In a clustered lock, it doesn't have an auto-release "lifespan" like cache
                // entries,
                // but the node crash detection in JGroups will release the lock.
                acquired = lock.tryLock(1, TimeUnit.SECONDS).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Exception while trying to acquire lock for {}: {}", msg.getThreadId(), e.getMessage());
            }

            if (acquired) {
                // Lock acquired!
                try {
                    log.info("Acquired lock for threadId: {}. Processing message: {}", msg.getThreadId(),
                            msg.getMessageId());

                    // Call SOAP mock
                    String response = soapClient.readMessage(msg.getMessageId());
                    log.info("SOAP Response for {}: {}", msg.getMessageId(), response);

                    // Mark as processed
                    messageService.markAsProcessed(msg);

                } catch (Exception e) {
                    log.error("Error processing message {}", msg.getMessageId(), e);
                } finally {
                    // Release lock
                    lock.unlock().join();
                    log.info("Released lock for threadId: {}", msg.getThreadId());
                }
            } else {
                log.info("Could not acquire lock for threadId: {}. Another worker is processing it.",
                        msg.getThreadId());
            }
        }
    }
}
