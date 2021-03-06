package com.expedia.alertmanager.service.util;

import com.expedia.alertmanager.service.conf.ElasticSearchConfig;
import com.google.gson.JsonObject;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Util class to create index with mappings if not found.
 */
@Component
@Slf4j
public class IndexCreatorIfNotPresent implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private JestClientFactory clientFactory;

    @Autowired
    private ElasticSearchConfig elasticSearchConfig;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        JestClient client = clientFactory.getObject();
        if (elasticSearchConfig.isCreateIndexIfNotFound()) {
            try {
                Action action = new IndicesExists.Builder(elasticSearchConfig.getIndexName()).build();
                JestResult result = client.execute(action);

                if (!result.isSucceeded()) {
                    Map<String, Object> settings = new HashMap<>();
                    settings.put("number_of_shards", 2);

                    JsonObject docObject = new JsonObject();
                    docObject.addProperty("dynamic", "false");
                    docObject.add("properties", ElasticUtil.buildMappingsJson());
                    JsonObject mapObject = new JsonObject();
                    // Renaming this as this is _doc is not supported for ES version < 6.2
                    mapObject.add(elasticSearchConfig.getDocType(), docObject);

                    CreateIndex createIndex = new CreateIndex.Builder(elasticSearchConfig.getIndexName())
                        .settings(settings)
                        .mappings(mapObject.toString())
                        .build();

                    result = client.execute(createIndex);
                    if (!result.isSucceeded()) {
                        throw new RuntimeException(result.getErrorMessage());
                    }
                    log.info("Successfully created index : " + elasticSearchConfig.getIndexName());
                }
            } catch (IOException e) {
                log.error("Store subscriptions failed", e);
                throw new RuntimeException(e);
            }
        }
    }

}
