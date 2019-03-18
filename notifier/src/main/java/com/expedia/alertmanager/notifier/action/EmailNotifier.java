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

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.notifier.builder.MessageComposer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Slf4j
public class EmailNotifier implements Notifier {

    private final String from;
    private final String to;
    private final Session session;
    private MessageComposer emailComposer;

    public EmailNotifier(MessageComposer emailComposer, String from, String to,
                         String host, String port, String username, String password) {
        this.emailComposer = emailComposer;
        this.from = from;
        this.to = to;
        Properties properties = new Properties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.username", username);
        properties.put("mail.smtp.password", password);
        session = Session.getDefaultInstance(properties, null);
    }

    @Override
    public void notify(Alert alert) {
        log.info("Sending alert to " + to +  ". Alert is " + alert.toString());
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            ((MimeMessage) message).setSubject("[Alert Manager Alert] You have an Alert :" + alert.getName(), "UTF-8");
            ((MimeMessage) message).setText(emailComposer.buildContent(alert, "email-template.ftl"), "UTF-8", "html");
            Transport.send(message);
            log.info("Email sent successfully");
        } catch (MessagingException e) {
            log.error("Exception in sending email", e);
        }
    }

}
