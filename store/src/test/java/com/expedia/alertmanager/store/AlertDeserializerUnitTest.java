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

package com.expedia.alertmanager.store;

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.store.serde.AlertDeserializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Test;

public class AlertDeserializerUnitTest {

    @Test
    public void testNullDeserialization() {
        final Alert alert = new AlertDeserializer().deserialize("", null);
        Assert.assertNull(alert);
    }

    @Test
    public void testValidDeserialization() throws JsonProcessingException {
        final String alertJson = "{\"name\":\"a1\",\"labels\":{\"service\": \"svc\"},\"annotations\":{\"observedValue\":\"5\",\"expectedValue\":\"10\"},\"creationTime\":1542709994095,\"generatorURL\":null}";
        final Alert alert = new AlertDeserializer().deserialize("", alertJson.getBytes());
        Assert.assertNotNull(alert);
        Assert.assertEquals(alert.getAnnotations().size(), 2);
        Assert.assertEquals(alert.getAnnotations().get("expectedValue"), "10");
        Assert.assertEquals(alert.getAnnotations().get("observedValue"), "5");
        Assert.assertEquals(alert.getName(), "a1");
        Assert.assertEquals(alert.getCreationTime(), 1542709994095L);
        Assert.assertEquals(alert.getLabels().size(), 1);
        Assert.assertEquals(alert.getLabels().get("service"), "svc");
    }
}
