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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubscriptionController {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @RequestMapping(value = "/subscriptions", method = RequestMethod.POST)
    public Subscription createSubscription(SubscriptionRequest subscriptionRequest) {
        Subscription subscription = new Subscription(subscriptionRequest.getMetricId(),
            subscriptionRequest.getModelId(),
            new SubscriptionType(subscriptionRequest.getSubscriptionType()), subscriptionRequest.getTarget());
        return subscriptionRepository.save(subscription);
    }
}
