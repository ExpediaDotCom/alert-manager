package com.expedia.alertmanager.model;

import lombok.Data;

@Data
public class SubscriptionResponse extends BaseSubscription {
    private String id;
    private User owner;
}
