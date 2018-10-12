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

import com.expedia.alertmanager.dao.SubscriptionMetricDetectorMappingRepository;
import com.expedia.alertmanager.entity.Subscription;
import com.expedia.alertmanager.entity.SubscriptionMetricDetectorMapping;
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
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(SubscriptionController.class)
public class SubscriptionControllerTests {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private SubscriptionMetricDetectorMappingRepository subscriptionMetricDetectorMappingRepo;

    @Test
    public void givenACreateSubscriptionRequest_shouldPersistSubscriptions()
        throws Exception {

        //given a subscription request
        ArrayList<SubscriptionRequest> subscriptionRequestList = new ArrayList<>();
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setMetricId("metricId");
        subscriptionRequest.setDetectorId("detectorId");
        subscriptionRequest.setSubscriptionType("EMAIL");
        subscriptionRequest.setEndpoint("email@email.com");
        subscriptionRequest.setCreatedBy("user");
        subscriptionRequestList.add(subscriptionRequest);

        String content = new ObjectMapper().writeValueAsString(subscriptionRequestList);
        given(subscriptionMetricDetectorMappingRepo.save(any(SubscriptionMetricDetectorMapping.class)))
            .willReturn(new SubscriptionMetricDetectorMapping());

        //verify
        mvc.perform(post("/subscriptions")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        verify(subscriptionMetricDetectorMappingRepo).save(any(SubscriptionMetricDetectorMapping.class));
    }

    @Test
    public void givenAMetricIdAndDetectorId_shouldReturnSubscriptions()
        throws Exception {

        //given metric id and detector id
        given(subscriptionMetricDetectorMappingRepo.findByMetricIdAndDetectorId(
            "1075bc5daeb15245a1933a0344c5a23c",
            "b0987951-5db1-451e-861a-a7a5ac3285df"))
            .willReturn(
                Arrays.asList(new SubscriptionMetricDetectorMapping("1075bc5daeb15245a1933a0344c5a23c",
                    "b0987951-5db1-451e-861a-a7a5ac3285df", new Subscription(Subscription.EMAIL_TYPE,
                    "email@email.com"))));

        //verify
        mvc.perform(get("/subscriptions/1075bc5daeb15245a1933a0344c5a23c/b0987951-5db1-451e-861a-a7a5ac3285df")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].metricId").value("1075bc5daeb15245a1933a0344c5a23c"));
        verify(subscriptionMetricDetectorMappingRepo).findByMetricIdAndDetectorId(any(), any());
    }
}
