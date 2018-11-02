package com.expedia.alertmanager.model;

import lombok.Data;

@Data
public class CreateSubscriptionRequest extends BaseSubscription {
    private User owner;
}
