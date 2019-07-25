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

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.model.Dispatcher;
import com.expedia.alertmanager.model.SubscriptionResponse;
import com.expedia.alertmanager.notifier.action.Notifier;
import com.expedia.alertmanager.notifier.action.NotifierFactory;
import com.expedia.alertmanager.notifier.config.ApplicationConfig;
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
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class AlertProcessorTest {

    private AlertProcessor alertProcessor;

    @Mock
    private NotifierFactory notifierFactory;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private Notifier notifier;

    @Mock
    private AlertReadService alertsReadService;

    @Mock
    private ApplicationConfig applicationConfig;

    @Before
    public void setUp() {
        alertProcessor = new AlertProcessor(notifierFactory, subscriptionService,
            alertsReadService, applicationConfig);
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
        given(applicationConfig.getExpiryTimeInSec()).willReturn((System.currentTimeMillis()+1) / 1000);
        Alert alert = new Alert();
        alert.setLabels(Collections.emptyMap());
        alertProcessor.receive(alert);
        verify(notifier, times(2)).notify(alert);
    }

    @Test
    public void whenAlertRateLimiterApplied_AndAlertReceivedIsBlockedByRateLimiter_noneOfTheNotifiersAreInvoked() {
        when(applicationConfig.isRateLimitEnabled()).thenReturn(true);
        when(applicationConfig.getRateLimit()).thenReturn(10L);
        when(alertsReadService.getAlertsCountForToday()).thenReturn(10L);
        given(applicationConfig.getExpiryTimeInSec()).willReturn(300l);
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
        verify(notifier, times(0)).notify(alert);
    }

    @Test
    public void whenDeprecatedAlertIsReceived_noneOfTheNotifiersAreInvoked() {
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
        //expiry time set to 5 min
        given(applicationConfig.getExpiryTimeInSec()).willReturn(300l);
        //set creation time as 0 to consider as an expired one.
        alert.setCreationTime(0);
        alertProcessor.receive(alert);
        //no notifiers are invoked
        verify(notifier, times(0)).notify(alert);
    }
}
