package com.expedia.alertmanager.model;

import lombok.Data;

import java.util.Map;

@Data
public class MatchSubscriptionRequest {
    private Map<String, String> labels;
}
