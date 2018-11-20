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
package com.expedia.alertmanager.api.conf;

import com.expedia.alertmanager.model.Alert;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    @Bean
    JestClientFactory clientFactory(ElasticSearchConfig elasticSearchConfig) {
        JestClientFactory factory = new JestClientFactory();
        HttpClientConfig.Builder builder =
            new HttpClientConfig.Builder(elasticSearchConfig.getUrls())
                .multiThreaded(true)
                .discoveryEnabled(false)
                .connTimeout((int) elasticSearchConfig.getConnectionTimeout())
                .maxConnectionIdleTime(elasticSearchConfig.getMaxConnectionIdleTime(),
                    TimeUnit.SECONDS)
                .maxTotalConnection(elasticSearchConfig.getMaxTotalConnection())
                .readTimeout(elasticSearchConfig.getReadTimeout())
                .requestCompressionEnabled(elasticSearchConfig.isRequestCompression())
                .discoveryFrequency(1L, TimeUnit.MINUTES);

        if (elasticSearchConfig.getUsername() != null
                        && elasticSearchConfig.getPassword() != null) {
            builder.defaultCredentials(elasticSearchConfig.getUsername(), elasticSearchConfig.getPassword());
        }

        factory.setHttpClientConfig(builder.build());
        return factory;
    }

    @Bean
    public ProducerFactory<String, Alert> producerFactory(KafkaConfig kafkaConfig) {
        return new DefaultKafkaProducerFactory(kafkaConfig.getKafkaProducerConfig());
    }

    @Bean
    public KafkaTemplate<String, Alert> kafkaTemplate(KafkaConfig kafkaConfig) {
        return new KafkaTemplate<>(producerFactory(kafkaConfig));
    }
}
