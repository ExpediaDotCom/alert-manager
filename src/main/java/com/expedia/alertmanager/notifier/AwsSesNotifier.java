package com.expedia.alertmanager.notifier;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.expedia.alertmanager.temp.MappedMetricData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AwsSesNotifier implements Notifier {

    private final String to;
    private final String from;

    public AwsSesNotifier(String to, String from) {
        this.to = to;
        this.from = from;
    }

    @Override
    public void execute(MappedMetricData mappedMetricData) {
        try {
            final String subject = "Alert for " + mappedMetricData.getMetricData().getMetricDefinition().getKey();
            final String body = "Anomaly Result " + mappedMetricData.getAnomalyResult().toString();
            AmazonSimpleEmailService client =
                AmazonSimpleEmailServiceClientBuilder.standard()
                    .withRegion(Regions.US_WEST_2).build();
            SendEmailRequest request = new SendEmailRequest()
                .withDestination(
                    new Destination().withToAddresses(to))
                .withMessage(new Message()
                    .withBody(new Body()
                        .withHtml(new Content()
                            .withCharset("UTF-8").withData(body))
                        .withText(new Content()
                            .withCharset("UTF-8").withData(body)))
                    .withSubject(new Content()
                        .withCharset("UTF-8").withData(subject)))
                .withSource(from);
            final SendEmailResult sendEmailResult = client.sendEmail(request);
            log.info("Email sent status: " + sendEmailResult);

        } catch (Exception ex) {
            log.info("SES email failed to send", ex);
        }
    }


}
