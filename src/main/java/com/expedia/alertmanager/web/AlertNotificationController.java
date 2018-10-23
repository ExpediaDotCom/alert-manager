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
import com.expedia.alertmanager.entity.SubscriptionMetricDetectorMapping;
import com.expedia.alertmanager.notifier.NotifierFactory;
import com.expedia.alertmanager.temp.MappedMetricData;
import com.expedia.metrics.IdFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
public class AlertNotificationController {

    @Autowired
    private SubscriptionMetricDetectorMappingRepository subscriptionMetricDetectorMappingRepo;

    @Autowired
    private NotifierFactory notifierFactory;

    @Autowired
    private IdFactory idFactory;

    @RequestMapping(value = "/alerts", method = RequestMethod.POST)
    public ResponseEntity notifyAlert(@RequestBody MappedMetricData mappedMetricData) {
        log.info("Received alert : {}", mappedMetricData);
        String detectorId = mappedMetricData.getDetectorUuid().toString();
        String metricId = idFactory.getId(mappedMetricData.getMetricData().getMetricDefinition());
        //TODO - remove these, added temporarily
        log.info("Metric Id : {}", metricId);
        List<SubscriptionMetricDetectorMapping> subscriptnMetricDetectrMappings
            = subscriptionMetricDetectorMappingRepo.findByMetricIdAndDetectorId(metricId, detectorId);
        log.info("Subscription Details : {}", Arrays.toString(subscriptnMetricDetectrMappings.toArray()));
        subscriptnMetricDetectrMappings.forEach(subscriptnMetricDetectrMapping -> {
            notifierFactory.createNotifier(subscriptnMetricDetectrMapping.getSubscription()).execute(mappedMetricData);
        });
        return new ResponseEntity(HttpStatus.OK);
    }
}
