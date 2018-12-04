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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class AlertProcessorTest {

    private AlertProcessor alertProcessor;

    @Mock
    private NotifierFactory notifierFactory;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private Notifier notifier;

    @Before
    public void setUp() {
        alertProcessor = new AlertProcessor(notifierFactory, subscriptionService);
    }

    @Test
    public void whenAnAlertIsReceived_shouldInvokeTheCorrespondingNotifier() {
        Dispatcher emailDispatcher = new Dispatcher();
        emailDispatcher.setType(Dispatcher.Type.EMAIL);
        emailDispatcher.setEndpoint("email@email.com");
        Dispatcher slackDispatcher = new Dispatcher();
        emailDispatcher.setType(Dispatcher.Type.SLACK);
        emailDispatcher.setEndpoint("#channel");
        SubscriptionResponse subscriptionResponse = new SubscriptionResponse();
        subscriptionResponse.setDispatchers(Arrays.asList(emailDispatcher, slackDispatcher));
        given(subscriptionService.getSubscriptions(anyMap())).willReturn(Arrays.asList(subscriptionResponse));
        given(notifierFactory.getNotifier(emailDispatcher)).willReturn(notifier);
        given(notifierFactory.getNotifier(slackDispatcher)).willReturn(notifier);
        Alert alert = new Alert();
        alert.setLabels(Collections.emptyMap());
        alertProcessor.receive(alert);
        verify(notifier, times(2)).notify(alert);
    }
}
