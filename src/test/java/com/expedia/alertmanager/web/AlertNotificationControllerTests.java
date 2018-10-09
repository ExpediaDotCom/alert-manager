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

import com.expedia.alertmanager.dao.SubscriptionRepository;
import com.expedia.alertmanager.entity.Subscription;
import com.expedia.alertmanager.entity.SubscriptionType;
import com.expedia.alertmanager.notifier.Notifier;
import com.expedia.alertmanager.notifier.NotifierFactory;
import com.expedia.alertmanager.temp.AnomalyLevel;
import com.expedia.alertmanager.temp.AnomalyResult;
import com.expedia.alertmanager.temp.MappedMetricData;
import com.expedia.metrics.MetricData;
import com.expedia.metrics.MetricDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(AlertNotificationController.class)
public class AlertNotificationControllerTests {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private NotifierFactory notifierFactory;

    @MockBean
    private SubscriptionRepository subscriptionRepository;

    @Test
    public void givenAnAlert_whenASubscriptionPresent_shouldInvokeNotifier()
        throws Exception {

        //given an alert
        MappedMetricData mappedMetricData = new MappedMetricData();
        MetricDefinition metricDefinition = new MetricDefinition("test");
        MetricData metricData = new MetricData(metricDefinition, 1, 10_000);
        mappedMetricData.setMetricData(metricData);
        mappedMetricData.setDetectorType("cusum");
        mappedMetricData.setDetectorUuid(UUID.randomUUID());
        AnomalyResult anomalyResult = new AnomalyResult();
        anomalyResult.setAnomalyLevel(AnomalyLevel.STRONG);
        anomalyResult.setDetectorUUID(mappedMetricData.getDetectorUuid());
        anomalyResult.setMetricData(metricData);
        mappedMetricData.setAnomalyResult(anomalyResult);

        //when email subscription
        List<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(new Subscription("1", "1", new SubscriptionType("email"),
            "email@email.com"));
        given(subscriptionRepository.findByMetricIdAndModelId("1", "1")).willReturn(subscriptions);
        Notifier mockNotifier = mock(Notifier.class);
        given(notifierFactory.createNotifier(any())).willReturn(mockNotifier);

        String content = new ObjectMapper().writeValueAsString(mappedMetricData);
        mvc.perform(post("/alerts", content)
            .content(content)
            .contentType(MediaType.ALL))
            .andExpect(status().isOk());
        verify(mockNotifier).execute(any());
    }
}
