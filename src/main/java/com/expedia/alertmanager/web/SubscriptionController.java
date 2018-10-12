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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class SubscriptionController {

    @Autowired
    private SubscriptionMetricDetectorMappingRepository subscriptionMetricDetectorMappingRepo;

    @RequestMapping(value = "/subscriptions", method = {RequestMethod.POST, RequestMethod.PUT})
    public List<SubscriptionMetricDetectorMapping> createSubscriptions(
        @RequestBody List<SubscriptionRequest> subscriptions) {
        List<SubscriptionMetricDetectorMapping> subscriptionResult =  new ArrayList<>();
        subscriptions.forEach(subscriptionRequest -> {
            Subscription subscription = new Subscription(subscriptionRequest.getSubscriptionType(),
                subscriptionRequest.getEndpoint());
            SubscriptionMetricDetectorMapping subscriptionMetricDetectorMapping =
                new SubscriptionMetricDetectorMapping(subscriptionRequest.getMetricId(),
                    subscriptionRequest.getDetectorId(), subscription);
            subscriptionResult.add(subscriptionMetricDetectorMappingRepo.save(subscriptionMetricDetectorMapping));
        });
        return subscriptionResult;
    }

    @RequestMapping(value = "/subscriptions/{metricId}/{detectorId}", method = RequestMethod.GET)
    public List<SubscriptionMetricDetectorMapping> getSubscriptions(@PathVariable String metricId,
                                                                    @PathVariable String detectorId) {
        return subscriptionMetricDetectorMappingRepo.findByMetricIdAndDetectorId(metricId, detectorId);
    }
}
