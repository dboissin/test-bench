package dev.ceven.petapp.service;

import dev.ceven.petapp.entity.ProcessingMessage;
import dev.ceven.petapp.repository.ProcessingMessageRepository;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lock.EmbeddedClusteredLockManagerFactory;
import org.infinispan.lock.api.ClusteredLock;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class InfinispanDistributedLockIntegrationTest {

    @Autowired
    private EmbeddedCacheManager cacheManagerNode1;

    @Autowired
    private ClusteredLockManager lockManagerNode1;

    @MockBean
    private ProcessingMessageService messageService;

    @MockBean
    private SoapMessageClient soapClient;

    @Autowired
    private MessageWorker messageWorkerNode1;

    private EmbeddedCacheManager cacheManagerNode2;
    private ClusteredLockManager lockManagerNode2;
    private MessageWorker messageWorkerNode2;

    @BeforeEach
    void setUp() throws Exception {
        // Spin up a second EmbeddedCacheManager in the same JVM to simulate a second
        // node in the cluster
        System.setProperty("java.net.preferIPv4Stack", "true");
        GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
        global.transport()
                .clusterName("pet-app-cluster")
                .defaultTransport()
                .addProperty("configurationFile", "jgroups-tcp.xml");
        cacheManagerNode2 = new DefaultCacheManager(global.build(), true);
        lockManagerNode2 = EmbeddedClusteredLockManagerFactory.from(cacheManagerNode2);
        messageWorkerNode2 = new MessageWorker(messageService, soapClient, lockManagerNode2);

        // Wait for cluster to form (2 nodes)
        long startTime = System.currentTimeMillis();
        while (cacheManagerNode1.getMembers().size() < 2 && (System.currentTimeMillis() - startTime) < 10000) {
            Thread.sleep(500);
        }
    }

    @AfterEach
    void tearDown() {
        if (cacheManagerNode2 != null) {
            cacheManagerNode2.stop();
        }
    }

    @Test
    void testConcurrentProcessing_AcrossCluster_OnlyOneProcesses() throws InterruptedException {
        // Verify cluster formation
        assertTrue(cacheManagerNode1.getMembers().size() >= 2, "Cluster should have formed with at least 2 members");

        // Prepare DB mock
        ProcessingMessage msg = new ProcessingMessage();
        msg.setMessageId("msg-ti-cluster");
        msg.setThreadId("thread-ti-cluster");
        msg.setStatus("PENDING");

        when(messageService.getPendingMessages()).thenReturn(Collections.singletonList(msg));

        // Mock SOAP Client - delay must be longer than tryLock timeout (1s)
        // so the second node's tryLock times out while the lock is still held
        AtomicInteger processCount = new AtomicInteger(0);
        when(soapClient.readMessage(anyString())).thenAnswer(invocation -> {
            processCount.incrementAndGet();
            Thread.sleep(3000); // simulate work (longer than tryLock 1s timeout)
            return "SUCCESS";
        });

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        // Node 1 attempts to process
        executorService.submit(() -> {
            try {
                latch.await();
                messageWorkerNode1.processMessages();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        // Node 2 attempts to process concurrently
        executorService.submit(() -> {
            try {
                latch.await();
                messageWorkerNode2.processMessages();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        // Release threads simultaneously
        latch.countDown();

        // Wait for both to finish
        doneLatch.await(15, TimeUnit.SECONDS);

        // Verify only 1 successfully called soapClient due to the Clustered Lock
        // spanning the two embedded nodes
        assertEquals(1, processCount.get(), "The message should only be processed exactly once across the cluster.");
    }
}
