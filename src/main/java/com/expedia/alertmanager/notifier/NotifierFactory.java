package com.expedia.alertmanager.notifier;

import com.expedia.alertmanager.entity.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class NotifierFactory {

    @Autowired
    private JavaMailSender emailSender;

    public Notifier createNotifier(Subscription subscription) {
        switch (subscription.getSubscriptionType().getType()) {
            case "email":
                return new EmailNotifier(emailSender, subscription.getTarget());
            case "pd":
                return new PagerDutyNotifier("", subscription.getTarget());
            default:
                throw new RuntimeException("No Notifier Found for the subscription type");
        }
    }
}
