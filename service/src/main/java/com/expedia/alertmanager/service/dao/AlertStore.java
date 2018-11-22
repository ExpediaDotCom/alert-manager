/*
 * Copyright 2018 Expedia Group, Inc.
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

import com.expedia.alertmanager.service.conf.AlertStoreConfig;
import com.expedia.alertmanager.service.conf.KafkaConfig;
import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.model.store.Store;
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
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AlertStore {
    private final static Logger LOGGER = LoggerFactory.getLogger(AlertStore.class);

    private final List<Store> stores;
    private KafkaTemplate<String, Alert> kafkaTemplate;
    private final String topic;

    @Autowired
    public AlertStore(final KafkaConfig kafkaConfig,
                      final KafkaTemplate<String, Alert> kafkaTemplate,
                      final AlertStoreConfig alertStoreConfig) throws IOException {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = kafkaConfig.getProducerTopic();
        this.stores = loadAndInitStoragePlugin(alertStoreConfig);
    }

    private List<Store> loadAndInitStoragePlugin(AlertStoreConfig storeConfig) throws IOException {
        final List<Store> stores = new ArrayList<>();
        for (AlertStoreConfig.PluginConfig cfg : storeConfig.getPlugins()) {
            final String pluginJarFileName = cfg.getJarName().toLowerCase();

            final URL[] urls = new URL[1];
            File pluginDir = new File(storeConfig.getPluginDirectory());
            File[] plugins = pluginDir.listFiles(file -> file.getName().toLowerCase().equals(pluginJarFileName));

            if (plugins == null || plugins.length == 0) {
                throw new RuntimeException(
                        String.format("Fail to find the plugin with jarName=%s in the directory=%s",
                                pluginJarFileName,
                                storeConfig.getPluginDirectory()));
            }

            for (int i = 0; i < plugins.length; i++) {
                urls[i] = plugins[i].toURI().toURL();
            }

            final URLClassLoader ucl = new URLClassLoader(urls);
            final ServiceLoader<Store> loader = ServiceLoader.load(Store.class, ucl);

            // load and initialize the plugin
            final Store store = loader.iterator().next();
            store.init(cfg.getConf());
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

        for (final Store store : stores) {
            store.read(labels, from, to, size, (receivedAlerts, ex) -> {
                synchronized (alerts) {
                    if (ex == null) {
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
