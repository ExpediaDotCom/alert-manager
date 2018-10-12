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
import com.expedia.alertmanager.temp.JsonPojoDeserializer;
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

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class AlertNotificationController {

    @Autowired
    private SubscriptionMetricDetectorMappingRepository subscriptionMetricDetectorMappingRepo;

    @Autowired
    private NotifierFactory notifierFactory;

    @Autowired
    private IdFactory idFactory;

    private final JsonPojoDeserializer<MappedMetricData> jsonPojoDeserializer = new JsonPojoDeserializer();

    @PostConstruct
    public void init() {
        Map<String, Class> configs = new HashMap<>();
        configs.put("JsonPojoClass", MappedMetricData.class);
        jsonPojoDeserializer.configure(configs);
    }

    @RequestMapping(value = "/alerts", method = RequestMethod.POST)
    public ResponseEntity notifyAlert(@RequestBody String mappedMetricDataJson) {
        log.info("Received alert : {}", mappedMetricDataJson);
        MappedMetricData mappedMetricData = deserialize(mappedMetricDataJson);
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

    //FIXME - we need to see if we can do the deserialization using the default deserializer provided by spring
    private MappedMetricData deserialize(String mappedMetricDataJson) {
        return jsonPojoDeserializer.deserialize(mappedMetricDataJson.getBytes());
    }
}
