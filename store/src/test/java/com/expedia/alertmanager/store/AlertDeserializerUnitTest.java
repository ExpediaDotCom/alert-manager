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
        final String alertJson = "{\"name\":\"a1\",\"labels\":{\"service\": \"svc\"},\"annotations\":null,\"observedValue\":\"5\",\"expectedValue\":\"10\",\"startTime\":1542709994095,\"generatorURL\":null}";
        final Alert alert = new AlertDeserializer().deserialize("", alertJson.getBytes());
        Assert.assertNotNull(alert);
        Assert.assertEquals(alert.getExpectedValue(), "10");
        Assert.assertEquals(alert.getObservedValue(), "5");
        Assert.assertEquals(alert.getName(), "a1");
        Assert.assertEquals(alert.getStartTime(), 1542709994095L);
        Assert.assertEquals(alert.getLabels().size(), 1);
        Assert.assertEquals(alert.getLabels().get("service"), "svc");
    }
}
