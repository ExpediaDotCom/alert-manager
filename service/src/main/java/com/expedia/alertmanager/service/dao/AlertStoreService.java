/*
 * Copyright 2018-2019 Expedia Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.expedia.alertmanager.service.dao;

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.model.store.AlertStore;
import com.expedia.alertmanager.service.conf.AlertStoreConfig;
import com.expedia.alertmanager.service.conf.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AlertStoreService {
    private final static Logger LOGGER = LoggerFactory.getLogger(AlertStoreService.class);

    private final List<AlertStore> stores;
    private KafkaTemplate<String, Alert> kafkaTemplate;
    private final String topic;

    @Autowired
    public AlertStoreService(final KafkaConfig kafkaConfig,
                      final KafkaTemplate<String, Alert> kafkaTemplate,
                      final AlertStoreConfig alertStoreConfig) throws IOException {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = kafkaConfig.getProducerTopic();
        this.stores = loadAndInitStoragePlugin(alertStoreConfig);
    }

    private List<AlertStore> loadAndInitStoragePlugin(AlertStoreConfig storeConfig) throws IOException {
        final List<AlertStore> stores = new ArrayList<>(storeConfig.getPlugins().size());
        for (AlertStoreConfig.PluginConfig cfg : storeConfig.getPlugins()) {
            final String pluginJarFileName = cfg.getJarName().toLowerCase();

            File pluginDir = new File(storeConfig.getPluginDirectory());
            File[] plugins = pluginDir.listFiles(file -> file.getName().equalsIgnoreCase(pluginJarFileName));

            if (plugins == null || plugins.length != 1) {
                throw new RuntimeException(
                        String.format("Fail to find the plugin with jarName=%s in the directory=%s",
                                pluginJarFileName,
                                storeConfig.getPluginDirectory()));
            }

            final URL[] urls = new URL[] { plugins[0].toURI().toURL() };
            final URLClassLoader ucl = new URLClassLoader(urls, AlertStore.class.getClassLoader());
            final ServiceLoader<AlertStore> loader = ServiceLoader.load(AlertStore.class, ucl);

            // load and initialize the plugin
            final AlertStore store = loader.iterator().next();
            final Map<String, Object> config = new HashMap<>();
            config.put("host", cfg.getHost());
            if (cfg.getConfig() != null) {
                config.putAll(cfg.getConfig());
            }
            store.init(config);
            stores.add(store);
        }
        return stores;
    }

    public void saveAlerts(List<Alert> alerts) {
        alerts.forEach(alert -> kafkaTemplate.send(topic, alert));
    }

    public CompletableFuture<List<Alert>> search(final Map<String, String> labels,
                                                 final long from,
                                                 final long to,
                                                 final int size) throws IOException {

        final CompletableFuture<List<Alert>> response = new CompletableFuture<>();
        final List<Alert> alerts = new ArrayList<>();
        final AtomicInteger waitForStores = new AtomicInteger(stores.size());

        for (final AlertStore store : stores) {
            store.read(labels, from, to, size, (receivedAlerts, ex) -> {
                synchronized (alerts) {
                    if (ex.getMessage() == "None") {
                        receivedAlerts.forEach(a -> alerts.add(a.getAlert()));
                        if (waitForStores.decrementAndGet() == 0) {
                            response.complete(alerts);
                        }
                    } else {
                        if (waitForStores.getAndSet(0) != 0) {
                            LOGGER.error("Fail to fetch alerts from the store with error", ex);
                            response.completeExceptionally(ex);
                        }
                    }
                }
            });
        }
        return response;
    }
}
