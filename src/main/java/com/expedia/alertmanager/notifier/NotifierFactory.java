package com.expedia.alertmanager.notifier;

import com.expedia.alertmanager.entity.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class NotifierFactory {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.fromAddress}")
    private String fromEmail;

    public Notifier createNotifier(Subscription subscription) {
        switch (subscription.getType()) {
            case Subscription.EMAIL_TYPE:
                return new EmailNotifier(emailSender, subscription.getEndpoint(), fromEmail);
            case Subscription.PD_TYPE:
                return new PagerDutyNotifier("", subscription.getEndpoint());
            default:
                throw new RuntimeException("No Notifier Found for the subscription type");
        }
    }
}
