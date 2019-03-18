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

import com.expedia.alertmanager.model.SearchSubscriptionRequest;
import com.expedia.alertmanager.model.SubscriptionResponse;
import com.expedia.alertmanager.notifier.config.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.util.List;
import java.util.Map;

@Component
public class SubscriptionService {
    private final RestTemplate restTemplate;
    private final String subscriptionSerUrl;

    @Autowired
    public SubscriptionService(RestTemplate restTemplate, ApplicationConfig applicationConfig) {
        this.restTemplate = restTemplate;
        this.subscriptionSerUrl = applicationConfig.getSubscriptionSerUrl();
    }

    //FIXME- This is a temporary code and will be removed.
    @PostConstruct
    public void disableSSLCertificateCheck() {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }

    public List<SubscriptionResponse> getSubscriptions(Map<String, String> labels) {
        SearchSubscriptionRequest rq = new SearchSubscriptionRequest();
        rq.setLabels(labels);
        HttpEntity<SearchSubscriptionRequest> request = new HttpEntity<>(rq);
        ResponseEntity<List<SubscriptionResponse>> subscriptionResponses =
            restTemplate.exchange(this.subscriptionSerUrl, HttpMethod.POST,
                request, new ParameterizedTypeReference<List<SubscriptionResponse>>(){});
        return subscriptionResponses.getBody();
    }
}
