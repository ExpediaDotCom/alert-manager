package com.expedia.alertmanager.web;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class UpdateSubscriptionRequest extends SubscriptionRequest {
    private Long id;
}
