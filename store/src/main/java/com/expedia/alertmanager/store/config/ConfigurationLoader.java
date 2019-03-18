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

package com.expedia.alertmanager.store.config;

import com.expedia.alertmanager.store.App;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ConfigurationLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationLoader.class);

    public static StoreConfig loadConfig(final File configFile) throws IOException {
        final InputStream configStream;
        if (configFile == null) {
            configStream = App.class.getResourceAsStream("/application.yaml");
        } else {
            LOGGER.info("Loading the configuration from file file {}", configFile.getAbsolutePath());
            configStream = new FileInputStream(configFile);
        }

        try {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(configStream, StoreConfig.class);
        } finally {
            if (configStream != null) {
                try {
                    configStream.close();
                } catch (Exception ex) {
                    LOGGER.error("Fail to close the config stream with error", ex);
                }
            }
        }
    }
}
