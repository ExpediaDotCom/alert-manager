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

package com.expedia.alertmanager.store.serde;

import com.expedia.alertmanager.model.Alert;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AlertDeserializer implements Deserializer<Alert> {
    private final static Logger LOGGER = LoggerFactory.getLogger(AlertDeserializer.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> map, boolean b) {
        /* nothing */
    }

    @Override
    public Alert deserialize(String topic, byte[] bytes) {
        try {
            return mapper.readValue(bytes, Alert.class);
        } catch (Exception ex) {
            LOGGER.error("Fail to deserialize the incoming bytes {} to alert", new String(bytes), ex);
            return null;
        }
    }

    @Override
    public void close() {
        /* nothing */
    }
}
