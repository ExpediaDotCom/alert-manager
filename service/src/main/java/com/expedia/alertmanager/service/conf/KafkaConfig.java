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
package com.expedia.alertmanager.service.conf;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KafkaConfig {

    @Value("${kafka.producer.bootstrap.servers}")
    private String producerBootstrapServer;
    @Value("${kafka.producer.topic}")
    private String producerTopic;
    @Value("${kafka.producer.client.id}")
    private String clientId;
    @Value("${kafka.producer.key.serializer}")
    private String keySerializer;
    @Value("${kafka.producer.value.serializer}")
    private String valueSerializer;
    @Value("${kafka.producer.request.timeout.ms}")
    private String producerReqTimeout;

    public Properties getKafkaProducerConfig() {
        final Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.producerBootstrapServer);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, this.clientId);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, this.keySerializer);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, this.valueSerializer);
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, this.producerReqTimeout);
        return properties;
    }

    public String getProducerTopic() {
        return this.producerTopic;
    }

}
