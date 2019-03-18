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

package com.expedia.alertmanager.store;

import com.expedia.alertmanager.model.store.AlertStore;
import com.expedia.alertmanager.store.config.ConfigurationLoader;
import com.expedia.alertmanager.store.config.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;

public class App {
    private final static Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        final StoreConfig cfg = loadConfig(args);
        final AlertStore store = loadAndInitializePlugin(cfg);
        final AlertStoreController pipeline =
                new AlertStoreController(cfg.getKafka(), store, new HealthController(cfg.getHealthStatusFile()));
        pipeline.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown hook is invoked, tearing down the application.");
            closeQuietly(pipeline);
            store.close();
        }));
    }

    private static StoreConfig loadConfig(String[] args) throws IOException {
        File configFile = null;
        if (args != null && args.length == 1 && !args[0].isEmpty()) {
            configFile = new File(args[0]);
        }
        return ConfigurationLoader.loadConfig(configFile);
    }

    private static AlertStore loadAndInitializePlugin(final StoreConfig cfg) throws Exception {
        final String pluginName = cfg.getPlugin().getName();
        final String pluginJarFileName = cfg.getPlugin().getJarName().toLowerCase();

        LOGGER.info("Loading the store plugin with name={}", pluginName);

        File pluginDir = new File(cfg.getPluginDirectory());
        File[] plugins = pluginDir.listFiles(file -> file.getName().toLowerCase().equals(pluginJarFileName));

        if (plugins == null || plugins.length != 1) {
            throw new RuntimeException(
                    String.format("Fail to find the plugin with jarName=%s in the directory=%s",
                            pluginJarFileName,
                            cfg.getPluginDirectory()));
        }

        final URL[] urls = new URL[] { plugins[0].toURI().toURL() };
        final URLClassLoader ucl = new URLClassLoader(urls);
        final ServiceLoader<AlertStore> loader = ServiceLoader.load(AlertStore.class, ucl);

        // load and initialize the plugin
        final AlertStore store = loader.iterator().next();
        store.init(cfg.getPlugin().getConf());

        LOGGER.info("Store plugin with name={} has been successfully loaded", pluginName);
        return store;
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch(Exception ex) {
            /* do nothing */
        }
    }
}
