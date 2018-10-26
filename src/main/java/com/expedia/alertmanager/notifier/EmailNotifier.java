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
import com.expedia.alertmanager.util.MailContentBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;

@Slf4j
public class EmailNotifier implements Notifier {

    private final JavaMailSender emailSender;
    private final String to;
    private final String from;
    private final MailContentBuilder mailContentBuilder;

    public EmailNotifier(JavaMailSender emailSender, String to, String from, MailContentBuilder mailContentBuilder) {
        this.emailSender = emailSender;
        this.to = to;
        this.from = from;
        this.mailContentBuilder = mailContentBuilder;
    }

    @Override
    public void execute(MappedMetricData mappedMetricData) {
        try {
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "utf-8");
            mimeMessage.setContent(mailContentBuilder.build(mappedMetricData), "text/html");
            helper.setTo(to);
            helper.setSubject(String.format(EMAIL_SUB,
                mappedMetricData.getMetricData().getMetricDefinition().getKey()));
            helper.setFrom(from);
            emailSender.send(mimeMessage);
        } catch (Exception ex) {
        log.error("Email failed to send", ex);
    }
    }
}
