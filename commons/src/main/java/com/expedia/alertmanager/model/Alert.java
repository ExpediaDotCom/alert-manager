package com.expedia.alertmanager.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class Alert {
    private String name;
    private Map<String, String> labels;
    private Map<String, String> annotations;
    private LocalDateTime dateTime;
}
