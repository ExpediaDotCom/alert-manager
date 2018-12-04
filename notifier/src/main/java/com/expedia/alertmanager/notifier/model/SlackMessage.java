package com.expedia.alertmanager.notifier.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SlackMessage {
    private String channel;
    private String text;
}
