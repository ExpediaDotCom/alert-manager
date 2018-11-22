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

package com.expedia.alertmanager.service.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "alert.store")
@Configuration
@Data
public class AlertStoreConfig {
    private String pluginDirectory;

    private List<PluginConfig> plugins;

    public static class PluginConfig {
        private String name;
        private String jarName;
        private Map<String, Object> conf;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getJarName() {
            return jarName;
        }

        public void setJarName(String jarName) {
            this.jarName = jarName;
        }

        public Map<String, Object> getConf() {
            return conf;
        }

        public void setConf(Map<String, Object> conf) {
            this.conf = conf;
        }
    }
}
