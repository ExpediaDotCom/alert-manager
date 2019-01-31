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

import com.expedia.alertmanager.model.Dispatcher;
import com.expedia.alertmanager.notifier.builder.MessageComposer;
import com.expedia.alertmanager.notifier.config.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NotifierFactory {

    @Autowired
    private ApplicationConfig applicationConfig;

    @Autowired
    private MessageComposer messageComposer;

    @Autowired
    private RestTemplate restTemplate;

    public Notifier getNotifier(Dispatcher dispatcher) {
        switch (dispatcher.getType()) {
            case EMAIL:
                return new AwsSesNotifier(messageComposer, applicationConfig.getFromEmail(), dispatcher);
            case SLACK:
                return new SlackNotifier(restTemplate, messageComposer, applicationConfig.getSlackUrl(),
                    applicationConfig.getSlackToken(), dispatcher.getEndpoint());
            default:
                throw new RuntimeException("Dispatcher type:" + dispatcher.getType() + "is not supported");
        }
    }
}
