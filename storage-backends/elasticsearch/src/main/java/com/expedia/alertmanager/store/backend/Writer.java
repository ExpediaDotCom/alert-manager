package com.expedia.alertmanager.store.backend;

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.model.store.AlertWithId;
import com.expedia.alertmanager.model.store.WriteCallback;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.expedia.alertmanager.store.backend.ElasticSearchStore.ES_INDEX_TYPE;

class Writer {
    private final static String INDEX_NAME_DATE_PATTERN = "yyyy-MM-dd";
    private final Map<String, Object> config;
    private final RestHighLevelClient client;
    private final String indexNamePrefix;
    private final Logger logger;

    Writer(final RestHighLevelClient client,
                  final Map<String, Object> config, 
                  final String indexNamePrefix, 
                  final Logger logger) {
        this.client = client;
        this.config = config;
        this.indexNamePrefix = indexNamePrefix;
        this.logger = logger;
    }

    void write(List<AlertWithId> alerts, WriteCallback callback) throws IOException {
        final BulkRequest bulkRequest = new BulkRequest();
        final SimpleDateFormat formatter = new SimpleDateFormat(INDEX_NAME_DATE_PATTERN);
        for (final AlertWithId alertWrapper : alerts) {
            final String idxName = indexName(formatter, alertWrapper.getAlert().getStartTime());
            final IndexRequest indexRequest = new IndexRequest(idxName, ES_INDEX_TYPE, alertWrapper.getId());
            indexRequest.source(convertAlertToMap(alertWrapper.getAlert()));
            bulkRequest.add(indexRequest);
        }

        this.client.bulkAsync(bulkRequest, new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse bulkItemResponses) {
                if (bulkItemResponses.hasFailures()) {
                    callback.onComplete(
                            new RuntimeException("Fail to execute the elastic search write with partial failures:"
                                    + bulkItemResponses.buildFailureMessage()));
                } else {
                    callback.onComplete(null);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onComplete(e);
            }
        });
    }

    private XContentBuilder convertAlertToMap(final Alert alert) throws IOException {
        return XContentFactory.jsonBuilder()
                .startObject()
                .field("name", alert.getName())
                .timeValueField("startTime", "startTime", alert.getStartTime())
                .field("annotations", alert.getAnnotations())
                .field("labels", alert.getLabels())
                .field("generatorURL", alert.getGeneratorURL())
                .field("observedValue", alert.getObservedValue())
                .field("expectedValue", alert.getExpectedValue())
                .endObject();
    }

    private String indexName(final SimpleDateFormat formatter, final long startTime) {
        return String.format("%s-%s", indexNamePrefix, formatter.format(new Date(startTime)));
    }
}
