package com.expedia.alertmanager.notifier;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import com.expedia.alertmanager.temp.MappedMetricData;
import lombok.extern.slf4j.Slf4j;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Properties;

@Slf4j
public class AwsSesNotifier implements Notifier {

    public static final String TEXT_PLAIN_CHARSET_UTF_8 = "text/plain; charset=UTF-8";
    public static final String TEXT_HTML_CHARSET_UTF_8 = "text/html; charset=UTF-8";
    public static final String BASE_64 = "base64";
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    private final String to;
    private final String from;

    public AwsSesNotifier(String to, String from) {
        this.to = to;
        this.from = from;
    }

    @Override
    public void execute(MappedMetricData mappedMetricData) {
        final String defaultCharSet = MimeUtility.getDefaultJavaCharset();
        try {
            final Session session = Session.getDefaultInstance(new Properties());
            final String subject = "Alert for " + mappedMetricData.getMetricData().getMetricDefinition().getKey();
            final MimeMessage message = new MimeMessage(session);
            message.setSubject(subject, "UTF-8");
            message.setFrom(new InternetAddress(to));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(from));
            message.setContent(buildMimeMultipart(mappedMetricData, defaultCharSet));

            final AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
                                                        .withRegion(Regions.US_WEST_2).build();

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);
            final RawMessage rawMessage =
                new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

            final SendRawEmailRequest rawEmailRequest =
                new SendRawEmailRequest(rawMessage);

            final SendRawEmailResult rawEmailResult = client.sendRawEmail(rawEmailRequest);
            log.info("Email sent status: " + rawEmailResult);

        } catch (Exception ex) {
            log.info("SES email failed to send", ex);
        }
    }

    private MimeMultipart buildMimeMultipart(MappedMetricData mappedMetricData, String defaultCharSet)
        throws MessagingException, UnsupportedEncodingException {
        final MimeBodyPart wrap = buildMimeBody(mappedMetricData, defaultCharSet);
        final MimeMultipart msg = new MimeMultipart("mixed");
        msg.addBodyPart(wrap);
        return msg;
    }

    private MimeBodyPart buildMimeBody(MappedMetricData mappedMetricData, String defaultCharSet)
        throws MessagingException, UnsupportedEncodingException {
        final MimeMultipart msgBody = new MimeMultipart("alternative");
        final MimeBodyPart wrap = new MimeBodyPart();
        final String body = "Anomaly Result " + mappedMetricData.getAnomalyResult().toString();
        final MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(MimeUtility
            .encodeText(body, defaultCharSet,"B"), TEXT_PLAIN_CHARSET_UTF_8);
        textPart.setHeader(CONTENT_TRANSFER_ENCODING, BASE_64);

        final MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(MimeUtility
            .encodeText(body, defaultCharSet,"B"), TEXT_HTML_CHARSET_UTF_8);
        htmlPart.setHeader(CONTENT_TRANSFER_ENCODING, BASE_64);

        msgBody.addBodyPart(textPart);
        msgBody.addBodyPart(htmlPart);

        wrap.setContent(msgBody);
        return wrap;
    }
}
