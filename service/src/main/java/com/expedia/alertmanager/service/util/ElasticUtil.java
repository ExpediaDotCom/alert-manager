package com.expedia.alertmanager.service.util;

import com.google.gson.JsonObject;

import static com.expedia.alertmanager.service.model.SubscriptionEntity.CREATE_TIME_KEYWORD;
import static com.expedia.alertmanager.service.model.SubscriptionEntity.DISPATCHERS_KEYWORD;
import static com.expedia.alertmanager.service.model.SubscriptionEntity.LAST_MOD_TIME_KEYWORD;
import static com.expedia.alertmanager.service.model.SubscriptionEntity.NAME;
import static com.expedia.alertmanager.service.model.SubscriptionEntity.QUERY_KEYWORD;
import static com.expedia.alertmanager.service.model.SubscriptionEntity.USER_KEYWORD;

public class ElasticUtil {
    public static JsonObject buildMappingsJson() {
        JsonObject userTypeObject = new JsonObject();
        userTypeObject.addProperty("type", "nested");
        userTypeObject.addProperty("dynamic", "true");
        JsonObject dispatcherTypeObject = new JsonObject();
        dispatcherTypeObject.addProperty("type", "object");
        JsonObject queryTypeObject = new JsonObject();
        queryTypeObject.addProperty("type", "percolator");
        JsonObject timeTypeObject = new JsonObject();
        timeTypeObject.addProperty("type", "long");
        JsonObject nameTypeObject = new JsonObject();
        nameTypeObject.addProperty("type", "keyword");

        JsonObject propObject = new JsonObject();
        propObject.add(USER_KEYWORD, userTypeObject);
        propObject.add(DISPATCHERS_KEYWORD, dispatcherTypeObject);
        propObject.add(QUERY_KEYWORD, queryTypeObject);
        propObject.add(LAST_MOD_TIME_KEYWORD, timeTypeObject);
        propObject.add(CREATE_TIME_KEYWORD, timeTypeObject);
        propObject.add(NAME, nameTypeObject);
        return propObject;
    }
}
