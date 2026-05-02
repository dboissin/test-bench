package dev.ceven.petapp.service;

import dev.ceven.petapp.entity.ProcessingMessage;
import dev.ceven.petapp.repository.ProcessingMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessingMessageServiceTest {

    @Mock
    private ProcessingMessageRepository repository;

    @Mock
    private SoapMessageClient soapClient;

    private ProcessingMessageService service;

    private static final String SOAP_RESPONSE = """
            <mes:getMessagesResponse xmlns:mes="http://example.com/messages">
                <mes:messageId>101-thread-A</mes:messageId>
                <mes:messageId>102-thread-A</mes:messageId>
                <mes:messageId>103-thread-A</mes:messageId>
                <mes:threadId>thread-A</mes:threadId>
            </mes:getMessagesResponse>
            """;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ProcessingMessageService(repository, soapClient);
    }

    @Test
    void getPendingMessages_returnsList() {
        ProcessingMessage msg = new ProcessingMessage();
        msg.setStatus("PENDING");
        when(repository.findByStatus("PENDING")).thenReturn(Collections.singletonList(msg));

        List<ProcessingMessage> result = service.getPendingMessages();

        assertEquals(1, result.size());
        verify(repository).findByStatus("PENDING");
    }

    @Test
    void markAsProcessed_setsStatusAndTimestamp() {
        ProcessingMessage msg = new ProcessingMessage();
        msg.setStatus("PENDING");

        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markAsProcessed(msg);

        assertEquals("PROCESSED", msg.getStatus());
        assertNotNull(msg.getProcessedAt());
        verify(repository).save(msg);
    }

    @Test
    void fetchMessages_callsSoapAndPersists() {
        when(soapClient.getMessages("thread-A")).thenReturn(SOAP_RESPONSE);
        when(repository.save(any(ProcessingMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ProcessingMessage> result = service.fetchMessages("thread-A");

        assertEquals(3, result.size());
        verify(soapClient).getMessages("thread-A");

        ArgumentCaptor<ProcessingMessage> captor = ArgumentCaptor.forClass(ProcessingMessage.class);
        verify(repository, times(3)).save(captor.capture());

        List<ProcessingMessage> saved = captor.getAllValues();
        assertEquals("101-thread-A", saved.get(0).getMessageId());
        assertEquals("102-thread-A", saved.get(1).getMessageId());
        assertEquals("103-thread-A", saved.get(2).getMessageId());

        for (ProcessingMessage msg : saved) {
            assertEquals("thread-A", msg.getThreadId());
            assertEquals("PENDING", msg.getStatus());
        }
    }

    @Test
    void fetchMessages_withInvalidXml_returnsEmptyList() {
        when(soapClient.getMessages("bad-thread")).thenReturn("not xml at all");

        List<ProcessingMessage> result = service.fetchMessages("bad-thread");

        assertTrue(result.isEmpty());
        verify(repository, never()).save(any());
    }

    @Test
    void fetchMessages_withEmptyResponse_returnsEmptyList() {
        String emptyResponse = """
                <mes:getMessagesResponse xmlns:mes="http://example.com/messages">
                    <mes:threadId>thread-B</mes:threadId>
                </mes:getMessagesResponse>
                """;
        when(soapClient.getMessages("thread-B")).thenReturn(emptyResponse);

        List<ProcessingMessage> result = service.fetchMessages("thread-B");

        assertTrue(result.isEmpty());
        verify(repository, never()).save(any());
    }
}
