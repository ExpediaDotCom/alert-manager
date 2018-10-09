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
package com.expedia.alertmanager.notifier;

import com.expedia.alertmanager.temp.MappedMetricData;
import com.expedia.alertmanager.util.JsonBuilder;
import com.expedia.alertmanager.util.PagerDutyJsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class PagerDutyNotifier implements Notifier {

    private final JsonBuilder<String> jsonBuilder;
    private final RestTemplate restTemplate;
    private final String pdUrl;
    private final String pdKey;

    /**
     * Constructs PagerDutyNotifier.
     *
     * @param pdUrl pager duty url
     * @param pdKey pager duty key
     */
    public PagerDutyNotifier(String pdUrl, String pdKey) {
        this.pdUrl = pdUrl;
        this.pdKey = pdKey;
        jsonBuilder = new PagerDutyJsonBuilder(pdKey);
        restTemplate = new RestTemplate();
    }

    @Override
    public void execute(MappedMetricData mappedMetricData) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String message = jsonBuilder.build(mappedMetricData);
        log.info("PagerDuty message: {}", message.replace(this.pdKey, "XXX"));
        HttpEntity<String> entity = new HttpEntity(message, headers);
        try {
            restTemplate.postForLocation(this.pdUrl, entity);
        } catch (Exception e) {
            log.error("PagerDuty invocation failed", e);
        }
    }
}
