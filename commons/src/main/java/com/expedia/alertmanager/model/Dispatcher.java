package com.expedia.alertmanager.model;

import lombok.Data;

@Data
public class Dispatcher {
    enum Type {
        EMAIL, SLACK
    }

    private Type type;
    private String endpoint;
}
