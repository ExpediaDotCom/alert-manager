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
package com.expedia.alertmanager.notifier.action;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.model.Dispatcher;
import com.expedia.alertmanager.model.util.EmailDispatcherHelper;
import com.expedia.alertmanager.notifier.builder.MessageComposer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AwsSesNotifier implements Notifier {


    private final String from;
    private final Dispatcher dispatcher;
    private MessageComposer emailComposer;

    public AwsSesNotifier(MessageComposer emailComposer, String from, Dispatcher dispatcher) {
        this.emailComposer = emailComposer;
        this.from = from;
        this.dispatcher = dispatcher;
    }

    @Override
    public void notify(Alert alert) {
        try {
            AmazonSimpleEmailService client =
                AmazonSimpleEmailServiceClientBuilder.standard()
                    .withRegion(Regions.US_WEST_2).build();
            //TODO - build email content from template
            SendEmailRequest request = new SendEmailRequest()
                .withDestination(
                    new Destination().withToAddresses(EmailDispatcherHelper.getToEmails(dispatcher)))
                .withMessage(new Message()
                    .withBody(new Body()
                        .withHtml(new Content()
                            .withCharset("UTF-8").withData(emailComposer.buildContent(alert, "email-template.ftl"))))
                    .withSubject(new Content()
                        .withCharset("UTF-8").withData("[Alert Manager Alert] You have an Alert :" + alert.getName())))
                .withSource(from);
            final SendEmailResult sendEmailResult = client.sendEmail(request);
            log.info("Email sent status: {}", sendEmailResult);

        } catch (Exception ex) {
            log.error("SES email failed to send", ex);
        }
    }
}
