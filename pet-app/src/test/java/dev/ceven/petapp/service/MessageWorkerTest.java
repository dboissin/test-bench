package dev.ceven.petapp.service;

import dev.ceven.petapp.entity.ProcessingMessage;

import org.infinispan.lock.api.ClusteredLock;
import org.infinispan.lock.api.ClusteredLockManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MessageWorkerTest {

    @Mock
    private ProcessingMessageService messageService;

    @Mock
    private SoapMessageClient soapClient;

    @Mock
    private ClusteredLockManager lockManager;

    @Mock
    private ClusteredLock lock;

    private MessageWorker messageWorker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        messageWorker = new MessageWorker(messageService, soapClient, lockManager);
        when(lockManager.get(anyString())).thenReturn(lock);
        when(lock.unlock()).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void processMessages_noPendingMessages() {
        when(messageService.getPendingMessages()).thenReturn(Collections.emptyList());
        messageWorker.processMessages();
        verify(lockManager, never()).get(anyString());
        verify(soapClient, never()).readMessage(anyString());
    }

    @Test
    void processMessages_acquiresLockAndProcesses() {
        ProcessingMessage msg = new ProcessingMessage();
        msg.setMessageId("msg-1");
        msg.setThreadId("thread-A");
        msg.setStatus("PENDING");

        when(messageService.getPendingMessages()).thenReturn(Collections.singletonList(msg));
        // Return true to indicate lock acquired
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(CompletableFuture.completedFuture(true));
        when(soapClient.readMessage("msg-1")).thenReturn("SOAP_RESPONSE");

        messageWorker.processMessages();

        verify(soapClient).readMessage("msg-1");
        verify(messageService).markAsProcessed(msg);
        verify(lock).unlock();
    }

    @Test
    void processMessages_failsToAcquireLock() {
        ProcessingMessage msg = new ProcessingMessage();
        msg.setMessageId("msg-1");
        msg.setThreadId("thread-A");
        msg.setStatus("PENDING");

        when(messageService.getPendingMessages()).thenReturn(Collections.singletonList(msg));
        // Return false to indicate lock is held by someone else
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(CompletableFuture.completedFuture(false));

        messageWorker.processMessages();

        // Should not process
        verify(soapClient, never()).readMessage(anyString());
        verify(messageService, never()).markAsProcessed(any());
        // Should not unlock if it didn't acquire it
        verify(lock, never()).unlock();
    }
}
