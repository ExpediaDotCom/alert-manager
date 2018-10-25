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
package com.expedia.alertmanager.web;

import com.expedia.alertmanager.conf.AppConf;
import com.expedia.alertmanager.dao.SubscriptionRepository;
import com.expedia.alertmanager.entity.Subscription;
import com.expedia.alertmanager.notifier.Notifier;
import com.expedia.alertmanager.notifier.NotifierFactory;
import com.expedia.alertmanager.temp.AnomalyLevel;
import com.expedia.alertmanager.temp.AnomalyResult;
import com.expedia.alertmanager.temp.MappedMetricData;
import com.expedia.metrics.IdFactory;
import com.expedia.metrics.MetricData;
import com.expedia.metrics.MetricDefinition;
import com.expedia.metrics.TagCollection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@Import(AppConf.class)
@WebMvcTest(AlertNotificationController.class)
public class AlertNotificationControllerTests {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private IdFactory idFactory;

    @MockBean
    private NotifierFactory notifierFactory;

    @MockBean
    private SubscriptionRepository subscriptionRepo;

    @Test
    public void givenAnAlert_whenASubscriptionPresent_shouldInvokeNotifier()
        throws Exception {

        //given an alert
        MappedMetricData mappedMetricData = new MappedMetricData();
        Map<String, String> tags = new HashMap();
        tags.put(MetricDefinition.MTYPE, "mtype");
        tags.put(MetricDefinition.UNIT, "unit");
        tags.put("org_id", "1");
        tags.put("interval", "30");
        TagCollection MINIMAL_TAGS = new TagCollection(tags);
        MetricDefinition metricDefinition = new MetricDefinition("test", MINIMAL_TAGS, TagCollection.EMPTY);
        MetricData metricData = new MetricData(metricDefinition, 1, 10_000);
        mappedMetricData.setMetricData(metricData);
        mappedMetricData.setDetectorType("cusum");
        UUID detectorId = UUID.randomUUID();
        mappedMetricData.setDetectorUuid(detectorId);
        AnomalyResult anomalyResult = new AnomalyResult();
        anomalyResult.setAnomalyLevel(AnomalyLevel.STRONG);
        anomalyResult.setDetectorUUID(mappedMetricData.getDetectorUuid());
        anomalyResult.setMetricData(metricData);
        mappedMetricData.setAnomalyResult(anomalyResult);

        //when email subscription
        List<Subscription> subscriptions = new ArrayList<>();
        String metricId = "1.1075bc5daeb15245a1933a0344c5a23c";
        subscriptions.add(new Subscription("1075bc5daeb15245a1933a0344c5a23c",
            "b0987951-5db1-451e-861a-a7a5ac3285df", "Booking Alert",
            "Changed Trend", Subscription.EMAIL_TYPE,
            "email@email.com", "user"));
        given(idFactory.getId(metricDefinition)).willReturn(metricId);
        given(subscriptionRepo.findByDetectorIdAndMetricId(detectorId.toString(), metricId))
            .willReturn(subscriptions);
        Notifier mockNotifier = mock(Notifier.class);
        given(notifierFactory.createNotifier(any())).willReturn(mockNotifier);

        String content = new ObjectMapper().writeValueAsString(mappedMetricData);
        mvc.perform(post("/alerts")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        verify(mockNotifier).execute(any());
    }
}
