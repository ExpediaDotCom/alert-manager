/*
 * Copyright 2018-2019 Expedia Group, Inc.
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

import com.expedia.alertmanager.notifier.config.ApplicationConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class AlertReadService {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final RestTemplate restTemplate;
    private final ApplicationConfig applicationConfig;

    @Autowired
    public AlertReadService(RestTemplate restTemplate, ApplicationConfig applicationConfig) {
        this.restTemplate = restTemplate;
        this.applicationConfig = applicationConfig;
    }

    public long getAlertsCountForToday() {
        //TODO - We need to invoke AM-Service api instead of directly invoking ES to get the count of alerts.
        ResponseEntity<CountResponse> countResponse =
            restTemplate.getForEntity(getCountUrl(applicationConfig.getAlertStoreEsUrl()), CountResponse.class);
        return countResponse.getBody().count;
    }

    private String getCountUrl(String alertStoreEsUrl) {
        return String.format(alertStoreEsUrl + "/alerts-%s/_count", dateFormat.format(new Date()));
    }

    @Data
    private static class CountResponse {
        public CountResponse() {}
        private long count;
    }
}
