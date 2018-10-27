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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(SubscriptionController.class)
public class SubscriptionControllerTests {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private SubscriptionRepository subscriptionRepo;

    @Test
    public void givenACreateSubscriptionRequest_shouldPersistSubscriptions()
        throws Exception {

        //given a subscription request
        ArrayList<SubscriptionRequest> subscriptionRequestList = new ArrayList<>();
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setMetricId("metricId");
        subscriptionRequest.setDetectorId("detectorId");
        subscriptionRequest.setName("name");
        subscriptionRequest.setDescription("description");
        subscriptionRequest.setType("EMAIL");
        subscriptionRequest.setEndpoint("email@email.com");
        subscriptionRequest.setOwner("user");
        subscriptionRequestList.add(subscriptionRequest);

        String content = new ObjectMapper().writeValueAsString(subscriptionRequestList);
        given(subscriptionRepo.save(any(Subscription.class)))
            .willReturn(Subscription.builder().build());

        //verify
        mvc.perform(post("/subscriptions")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        verify(subscriptionRepo).saveAll(any(Iterable.class));
    }

    @Test
    public void givenAnUpdateSubscriptionRequest_shouldPersistSubscriptions()
        throws Exception {

        //given an update subscription request
        ArrayList<UpdateSubscriptionRequest> updateSubscriptionRequests = new ArrayList<>();
        UpdateSubscriptionRequest updateSubscriptionRequest = new UpdateSubscriptionRequest();
        updateSubscriptionRequest.setId(10l);
        updateSubscriptionRequest.setMetricId("metricId");
        updateSubscriptionRequest.setDetectorId("detectorId");
        updateSubscriptionRequest.setName("name");
        updateSubscriptionRequest.setDescription("description");
        updateSubscriptionRequest.setType("EMAIL");
        updateSubscriptionRequest.setEndpoint("email@email.com");
        updateSubscriptionRequest.setOwner("user");
        updateSubscriptionRequests.add(updateSubscriptionRequest);

        String content = new ObjectMapper().writeValueAsString(updateSubscriptionRequests);
        given(subscriptionRepo.save(any(Subscription.class)))
            .willReturn(Subscription.builder().build());

        //verify
        mvc.perform(put("/subscriptions")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        verify(subscriptionRepo).saveAll(any(Iterable.class));
    }

    @Test
    public void givenAMetricIdAndDetectorId_shouldReturnSubscriptions()
        throws Exception {

        //given detector id and metric id
        given(subscriptionRepo.findByDetectorIdAndMetricId(
            "b0987951-5db1-451e-861a-a7a5ac3285df",
            "1075bc5daeb15245a1933a0344c5a23c"))
            .willReturn(
                Arrays.asList(Subscription.builder().metricId("1075bc5daeb15245a1933a0344c5a23c")
                    .detectorId("b0987951-5db1-451e-861a-a7a5ac3285df").name("Booking Alert")
                    .description("Changed Trend").type(Subscription.TYPE.EMAIL.name())
                    .endpoint("email@email.com").owner("user").build()));

        //verify
        mvc.perform(get("/subscriptions?detectorId=b0987951-5db1-451e-861a-a7a5ac3285df&metricId=1075bc5daeb15245a1933a0344c5a23c")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].metricId").value("1075bc5daeb15245a1933a0344c5a23c"));
        verify(subscriptionRepo).findByDetectorIdAndMetricId(any(), any());
    }

    @Test
    public void givenDetectorId_shouldReturnSubscriptions()
        throws Exception {

        //given detector id
        given(subscriptionRepo.findByDetectorId(
            "b0987951-5db1-451e-861a-a7a5ac3285df"))
            .willReturn(
                Arrays.asList(Subscription.builder().metricId("1075bc5daeb15245a1933a0344c5a23c")
                    .detectorId("b0987951-5db1-451e-861a-a7a5ac3285df").name("Booking Alert")
                    .description("Changed Trend").type(Subscription.TYPE.EMAIL.name())
                    .endpoint("email@email.com").owner("user").build()));

        //verify
        mvc.perform(get("/subscriptions?detectorId=b0987951-5db1-451e-861a-a7a5ac3285df")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].metricId").value("1075bc5daeb15245a1933a0344c5a23c"));
        verify(subscriptionRepo).findByDetectorId(any());
    }

    @Test
    public void givenOwner_shouldReturnSubscriptions()
        throws Exception {

        //given owner
        given(subscriptionRepo.findByOwner(
            "user"))
            .willReturn(
                Arrays.asList(Subscription.builder().metricId("1075bc5daeb15245a1933a0344c5a23c")
                    .detectorId("b0987951-5db1-451e-861a-a7a5ac3285df").name("Booking Alert")
                    .description("Changed Trend").type(Subscription.TYPE.EMAIL.name())
                    .endpoint("email@email.com").owner("user").build()));

        //verify
        mvc.perform(get("/subscriptions?owner=user")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].metricId").value("1075bc5daeb15245a1933a0344c5a23c"));
        verify(subscriptionRepo).findByOwner(any());
    }

    @Test
    public void apiInvocationWithWrongParamsShouldFail()
        throws Exception {

        //invoke with no parameter
        mvc.perform(get("/subscriptions")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}
