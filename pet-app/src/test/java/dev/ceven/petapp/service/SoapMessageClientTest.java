package dev.ceven.petapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.xml.transform.StringResult;

import javax.xml.transform.Source;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SoapMessageClientTest {

    @Mock
    private WebServiceTemplate webServiceTemplate;

    private SoapMessageClient soapMessageClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        soapMessageClient = new SoapMessageClient(webServiceTemplate);
    }

    @Test
    void testGetMessages() {
        when(webServiceTemplate.sendSourceAndReceiveToResult(any(Source.class), any(SoapActionCallback.class),
                any(StringResult.class)))
                .thenAnswer(invocation -> {
                    StringResult result = invocation.getArgument(2);
                    // Just simulating that WebServiceTemplate modified the result
                    return true;
                });

        String result = soapMessageClient.getMessages("thread-A");
        assertNotNull(result);
        verify(webServiceTemplate, times(1)).sendSourceAndReceiveToResult(any(Source.class),
                any(SoapActionCallback.class), any(StringResult.class));
    }

    @Test
    void testReadMessage() {
        when(webServiceTemplate.sendSourceAndReceiveToResult(any(Source.class), any(SoapActionCallback.class),
                any(StringResult.class)))
                .thenAnswer(invocation -> {
                    return true;
                });

        String result = soapMessageClient.readMessage("123");
        assertNotNull(result);
        verify(webServiceTemplate, times(1)).sendSourceAndReceiveToResult(any(Source.class),
                any(SoapActionCallback.class), any(StringResult.class));
    }
}
