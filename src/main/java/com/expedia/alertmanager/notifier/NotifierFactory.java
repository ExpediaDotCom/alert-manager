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

import com.expedia.alertmanager.entity.Subscription;
import com.expedia.alertmanager.util.MailContentBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class NotifierFactory {

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private MailContentBuilder mailContentBuilder;

    @Value("${spring.mail.fromAddress}")
    private String fromEmail;

    @Value("${aws.ses.enabled:true}")
    private boolean useAwsSes;

    public Notifier createNotifier(Subscription subscription) {
        switch (subscription.getType()) {
            case Subscription.EMAIL_TYPE:
                if (useAwsSes) {
                    return new AwsSesNotifier(subscription.getEndpoint(), fromEmail, mailContentBuilder);
                }
                else {
                    return new EmailNotifier(emailSender, subscription.getEndpoint(), fromEmail, mailContentBuilder);
                }
            case Subscription.PD_TYPE:
                return new PagerDutyNotifier("", subscription.getEndpoint());
            default:
                throw new RuntimeException("No Notifier Found for the subscription type");
        }
    }
}
