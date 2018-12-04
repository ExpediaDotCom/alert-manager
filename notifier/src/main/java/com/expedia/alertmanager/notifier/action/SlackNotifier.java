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
package com.expedia.alertmanager.notifier.action;

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.notifier.builder.MessageComposer;
import com.expedia.alertmanager.notifier.model.SlackMessage;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class SlackNotifier implements Notifier {

    private static final Gson gson = new Gson();
    private final RestTemplate restTemplate;
    private final String url;
    private final String token;
    private final String channel;
    private MessageComposer messageComposer;

    public SlackNotifier(RestTemplate restTemplate, MessageComposer messageComposer,
                         String url, String token, String channel) {
        this.restTemplate = restTemplate;
        this.messageComposer = messageComposer;
        this.url = url;
        this.token = token;
        this.channel = channel;
    }

    @Override
    public void notify(Alert alert) {
        //TODO - we need to parameterize the template
        SlackMessage slackMessage = new SlackMessage(channel,
            messageComposer.buildContent(alert, "slack-message-template.ftl"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(gson.toJson(slackMessage), headers);
        try {
            ResponseEntity responseEntity =
                restTemplate.exchange(url, HttpMethod.POST, entity, ResponseEntity.class);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                log.error("Slack notify failed");
            }
        } catch (HttpClientErrorException ex) {
            log.error("Slack invocation failed", ex);
        }
    }
}
