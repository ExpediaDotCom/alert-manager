package com.expedia.alertmanager.model;

import lombok.Data;

import java.util.List;

@Data
public abstract class BaseSubscription {
    private ExpressionTree expression;
    private List<Dispatcher> dispatchers;
}
