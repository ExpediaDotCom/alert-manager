package com.expedia.alertmanager.notifier.builder;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.expedia.alertmanager.model.Alert;
import freemarker.template.Configuration;

public class MessageComposerTest {

    private MessageComposer messageComposer;

    private Configuration freemarkerConfig;

    @Before
    public void setUp() throws IOException {
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_28);
        freemarkerConfig.setDirectoryForTemplateLoading(new File("../templates/"));
        messageComposer = new MessageComposer(freemarkerConfig);
    }

    @Test
    public void test_buildContent_For_Email_Template() {
        String out = messageComposer.buildContent(new Alert(), "email-template.ftl");
        assertTrue("Email template exists", out.contains("You have one alert."));
    }

    @Test
    public void test_buildContent_For_Slack_Template() {
        String out = messageComposer.buildContent(new Alert(), "slack-message-template.ftl");
        assertTrue("Slack template exists", out.contains("You have one alert."));
    }
}
