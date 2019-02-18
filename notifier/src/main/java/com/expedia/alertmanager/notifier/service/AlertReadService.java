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

import com.expedia.alertmanager.notifier.config.ElasticSearchConfig;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.core.Count;
import io.searchbox.core.CountResult;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Component
public class AlertReadService {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final RestTemplate restTemplate;
    private final ElasticSearchConfig elasticSearchConfig;
    private final JestClientFactory clientFactory;

    @Autowired
    public AlertReadService(RestTemplate restTemplate, ElasticSearchConfig elasticSearchConfig,
                            JestClientFactory clientFactory) {
        this.restTemplate = restTemplate;
        this.elasticSearchConfig = elasticSearchConfig;
        this.clientFactory = clientFactory;
    }

    //FIXME - AM notifier shouldn't directly invoke ES for alerts. All store interactions should go via AM store module.
    @Deprecated
    public long getAlertsCountForToday() {
        val client = clientFactory.getObject();
        val count = new Count.Builder()
            .addIndex(String.format("alerts-%s", dateFormat.format(new Date())))
            .addType(elasticSearchConfig.getDocType())
            .build();
        try {
            val result = client.execute(count);
            return result.getCount().longValue();
        }
        catch (Exception e) {
            log.error("Error while finding total alert count", e);
            throw new RuntimeException(e);
        }
        finally {
            try {
                client.close();
            } catch (IOException e) {
                log.error("Error while closing connection", e);
            }
        }
    }
}
