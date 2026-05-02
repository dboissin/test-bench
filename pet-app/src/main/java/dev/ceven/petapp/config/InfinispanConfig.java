package dev.ceven.petapp.config;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.lock.EmbeddedClusteredLockManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfinispanConfig {

    @Bean(destroyMethod = "stop")
    public EmbeddedCacheManager cacheManager() {
        // Ensure IPv4 stack for JGroups compatibility
        System.setProperty("java.net.preferIPv4Stack", "true");

        GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
        global.transport()
                .clusterName("pet-app-cluster")
                .defaultTransport()
                .addProperty("configurationFile", "jgroups-tcp.xml");
        return new DefaultCacheManager(global.build(), true);
    }

    @Bean
    public ClusteredLockManager clusteredLockManager(EmbeddedCacheManager cacheManager) {
        return EmbeddedClusteredLockManagerFactory.from(cacheManager);
    }
}
