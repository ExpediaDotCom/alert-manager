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
package com.expedia.alertmanager.api.dao;

import com.expedia.alertmanager.api.conf.KafkaConfig;
import com.expedia.alertmanager.model.Alert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertStore {

    private KafkaTemplate<String, Alert> kafkaTemplate;
    private final String topic;

    @Autowired
    public AlertStore(KafkaConfig kafkaConfig, KafkaTemplate<String, Alert> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = kafkaConfig.getProducerTopic();
    }

    public void saveAlerts(List<Alert> alerts) {
        alerts.forEach(alert -> {
            kafkaTemplate.send(topic, alert);
        });
    }
}
