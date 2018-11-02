package com.expedia.alertmanager.model;

import lombok.Data;

@Data
public class UpdateSubscriptionRequest extends BaseSubscription {
    private String id;
}
