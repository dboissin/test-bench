package dev.ceven.petapp.service;

import dev.ceven.petapp.entity.ProcessingMessage;
import dev.ceven.petapp.repository.ProcessingMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProcessingMessageService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingMessageService.class);

    private final ProcessingMessageRepository repository;
    private final SoapMessageClient soapClient;

    public ProcessingMessageService(ProcessingMessageRepository repository, SoapMessageClient soapClient) {
        this.repository = repository;
        this.soapClient = soapClient;
    }

    public List<ProcessingMessage> getPendingMessages() {
        return repository.findByStatus("PENDING");
    }

    @Transactional
    public void markAsProcessed(ProcessingMessage msg) {
        msg.setStatus("PROCESSED");
        msg.setProcessedAt(OffsetDateTime.now());
        repository.save(msg);
    }

    /**
     * Fetches messages from the SOAP WS for a given threadId,
     * parses the response to extract message IDs, and persists them
     * as PENDING entries in the processing_messages table.
     *
     * @return the list of persisted ProcessingMessage entities
     */
    @Transactional
    public List<ProcessingMessage> fetchMessages(String threadId) {
        log.info("Fetching messages for threadId: {}", threadId);
        String xmlResponse = soapClient.getMessages(threadId);
        log.info("Received SOAP response for threadId {}: {}", threadId, xmlResponse);

        List<String> messageIds = parseMessageIds(xmlResponse);
        String parsedThreadId = parseThreadId(xmlResponse, threadId);

        List<ProcessingMessage> saved = new ArrayList<>();
        for (String messageId : messageIds) {
            ProcessingMessage msg = new ProcessingMessage();
            msg.setMessageId(messageId);
            msg.setThreadId(parsedThreadId);
            msg.setStatus("PENDING");
            saved.add(repository.save(msg));
            log.info("Persisted PENDING message: messageId={}, threadId={}", messageId, parsedThreadId);
        }
        return saved;
    }

    private List<String> parseMessageIds(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            NodeList nodes = doc.getElementsByTagNameNS("http://example.com/messages", "messageId");
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                ids.add(nodes.item(i).getTextContent());
            }
            return ids;
        } catch (Exception e) {
            log.error("Failed to parse messageIds from SOAP response", e);
            return List.of();
        }
    }

    private String parseThreadId(String xml, String fallback) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            NodeList nodes = doc.getElementsByTagNameNS("http://example.com/messages", "threadId");
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent();
            }
        } catch (Exception e) {
            log.error("Failed to parse threadId from SOAP response", e);
        }
        return fallback;
    }
}
