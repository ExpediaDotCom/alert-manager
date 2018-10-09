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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class EmailNotifier implements Notifier {

    private JavaMailSender emailSender;
    private final String to;

    public EmailNotifier(JavaMailSender emailSender, String to) {
        this.emailSender = emailSender;
        this.to = to;
    }

    @Override
    public void execute(MappedMetricData mappedMetricData) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        //TODO - Use email template for email
        message.setSubject("Alert for " + mappedMetricData.getMetricData().getMetricDefinition().getKey());
        message.setText("Anomaly Result " + mappedMetricData.getAnomalyResult().toString());
        emailSender.send(message);
    }
}
