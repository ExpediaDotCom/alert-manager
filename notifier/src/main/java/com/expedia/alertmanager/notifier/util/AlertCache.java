package com.expedia.alertmanager.notifier.util;

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.notifier.config.ApplicationConfig;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AlertCache {

    @Autowired
    private ApplicationConfig applicationConfig;

    private Cache<Map<String, String>, Alert> cache = CacheBuilder.newBuilder()
        .expireAfterWrite(applicationConfig.getCacheInterval(), TimeUnit.SECONDS)
        .build();

    public Alert getAlert(Alert alert) {
        return cache.getIfPresent(alert.getLabels());
    }

    public void putAlert(Alert alert) {
        cache.put(alert.getLabels(), alert);
    }

}
