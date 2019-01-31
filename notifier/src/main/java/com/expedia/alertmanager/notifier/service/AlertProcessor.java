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
package com.expedia.alertmanager.notifier.service;

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.model.Dispatcher;
import com.expedia.alertmanager.model.SubscriptionResponse;
import com.expedia.alertmanager.notifier.action.Notifier;
import com.expedia.alertmanager.notifier.action.NotifierFactory;
import com.expedia.alertmanager.notifier.config.ApplicationConfig;
import com.expedia.alertmanager.notifier.util.AlertCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AlertProcessor {

    private final NotifierFactory notifierFactory;
    private final SubscriptionService subscriptionService;
    private final AlertCache alertCache;
    private ApplicationConfig applicationConfig;

    @Autowired
    public AlertProcessor(NotifierFactory notifierFactory,
                          SubscriptionService subscriptionService,
                          ApplicationConfig applicationConfig,
                          AlertCache alertCache) {
        this.notifierFactory = notifierFactory;
        this.subscriptionService = subscriptionService;
        this.applicationConfig = applicationConfig;
        this.alertCache = alertCache;
    }

    @KafkaListener(topics = "${kafka.topic}")
    public void receive(Alert alert) {
        log.info("received alert='{}'", alert);
        //FIXME - this is a temporary dedupe logic to reduce the duplicate email notifications.
        //we are using a cache to make sure that we send only 1 unique alert notification with in a given interval
        if (applicationConfig.isAlertCacheEnabled()) {
            if (alertCache.getAlert(alert) != null) {
                log.info("Duplicate alert {}", alert);
                return;
            } else {
                alertCache.putAlert(alert);
            }
        }

        List<SubscriptionResponse> subscriptionResponses = getSubscriptions(alert);
        log.info("Matching subscriptions='{}'", subscriptionResponses.toString());
        subscriptionResponses.forEach(subscriptionResponse -> {
            subscriptionResponse.getDispatchers().forEach(dispatcher -> {
                log.info("Matching dispatchers='{}'", dispatcher);
                Notifier notifier = getNotifier(dispatcher);
                notifier.notify(alert);
            });
        });
    }

    private Notifier getNotifier(Dispatcher dispatcher) {
        return this.notifierFactory.getNotifier(dispatcher);
    }

    private List<SubscriptionResponse> getSubscriptions(Alert alert) {
        return subscriptionService.getSubscriptions(alert.getLabels());
    }
}
