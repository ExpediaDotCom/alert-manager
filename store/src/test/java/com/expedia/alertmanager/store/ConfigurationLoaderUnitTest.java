package com.expedia.alertmanager.store;


import com.expedia.alertmanager.store.config.ConfigurationLoader;
import com.expedia.alertmanager.store.config.StoreConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationLoaderUnitTest {
    @Test
    public void testBaseConfiguration() throws IOException {
        final StoreConfig cfg = ConfigurationLoader.loadConfig(null);
        Assert.assertEquals(cfg.getPluginDirectory(), "storage-backends/elasticsearch/target");
        Assert.assertEquals(cfg.getPlugin().getJarName(), "elasticsearch-store-1.0.0-SNAPSHOT.jar");
        Assert.assertEquals(cfg.getPlugin().getName(), "elasticsearch");
    }

    @Test
    public void testConfigurationFromFile() throws IOException {

        final Path configFilePath = Paths.get("/tmp/config.yaml");
        final String config = "plugin.directory: \"storage-backends/elasticsearch\"\n" +
                "plugin:\n" +
                "   name: \"elasticsearch\"\n" +
                "   jar.name: \"elasticsearch-store.jar\"\n" +
                "kafka:\n" +
                "  topic: alerts\n" +
                "  stream.threads: 4\n" +
                "  consumer:\n" +
                "    bootstrap.servers: kafkasvc:9092\n" +
                "    auto.offset.reset: latest\n" +
                "    group.id: alert-manager-store\n" +
                "    enable.auto.commit: false";

        Files.write(configFilePath, config.getBytes("utf-8"));
        final StoreConfig cfg = ConfigurationLoader.loadConfig(new File("/tmp/config.yaml"));
        Assert.assertEquals(cfg.getPluginDirectory(), "storage-backends/elasticsearch");
        Assert.assertEquals(cfg.getPlugin().getJarName(), "elasticsearch-store.jar");
        Assert.assertEquals(cfg.getPlugin().getName(), "elasticsearch");
        Assert.assertEquals(cfg.getKafka().getTopic(), "alerts");
        Assert.assertEquals(cfg.getKafka().getStreamThreads(), 4);
        Assert.assertEquals(cfg.getKafka().getConsumer().get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG), "kafkasvc:9092");
        Assert.assertEquals(cfg.getKafka().getConsumer().get(ConsumerConfig.GROUP_ID_CONFIG), "alert-manager-store");
        Assert.assertEquals(cfg.getKafka().getConsumer().get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG), "latest");
        Assert.assertEquals(cfg.getKafka().getConsumer().get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG), false);
        Assert.assertEquals(cfg.getKafka().getMaxWakeups(), 10);
        Assert.assertEquals(cfg.getKafka().getWakeupTimeoutInMillis(), 3000);
        Assert.assertEquals(cfg.getKafka().getPollTimeoutMillis(), 2000);
        Files.delete(configFilePath);
    }
}
