package dev.ceven.petapp.service;

import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

import javax.xml.transform.Source;

@Service
public class SoapMessageClient {

    private final WebServiceTemplate webServiceTemplate;

    public SoapMessageClient(WebServiceTemplate webServiceTemplate) {
        this.webServiceTemplate = webServiceTemplate;
    }

    public String getMessages(String threadId) {
        String requestPayload = "<mes:getMessages xmlns:mes=\"http://example.com/messages\"><mes:threadId>" + threadId
                + "</mes:threadId></mes:getMessages>";
        Source requestSource = new StringSource(requestPayload);
        StringResult result = new StringResult();

        webServiceTemplate.sendSourceAndReceiveToResult(
                requestSource,
                new SoapActionCallback("getMessages"),
                result);
        return result.toString();
    }

    public String readMessage(String messageId) {
        String requestPayload = "<mes:readMessage xmlns:mes=\"http://example.com/messages\"><mes:messageId>" + messageId
                + "</mes:messageId></mes:readMessage>";
        Source requestSource = new StringSource(requestPayload);
        StringResult result = new StringResult();

        webServiceTemplate.sendSourceAndReceiveToResult(
                requestSource,
                new SoapActionCallback("readMessage"),
                result);
        return result.toString();
    }
}
