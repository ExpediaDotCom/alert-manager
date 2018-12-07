package com.expedia.alertmanager.store.backend;

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.model.store.AlertWithId;
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

import static com.expedia.alertmanager.store.backend.ElasticSearchStore.*;

class Writer {
    private final static long DEFAULT_RETRY_BACKOFF_MS = 200;
    private final static int DEFAULT_MAX_RETRIES = 50;

    private final static String INDEX_NAME_DATE_PATTERN = "yyyy-MM-dd";
    private final RestHighLevelClient client;
    private final String indexNamePrefix;
    private final Logger logger;
    private final int maxRetries;
    private final long retryBackOffMillis;

    Writer(final RestHighLevelClient client,
           final Map<String, Object> config,
           final String indexNamePrefix,
           final Logger logger) {
        this.client = client;
        this.indexNamePrefix = indexNamePrefix;
        this.logger = logger;
        this.maxRetries = Integer.parseInt(config.getOrDefault("max.retries", DEFAULT_MAX_RETRIES).toString());
        this.retryBackOffMillis = Long.parseLong(config.getOrDefault("retry.backoff.ms", DEFAULT_RETRY_BACKOFF_MS).toString());
    }

    void write(final List<AlertWithId> alerts, final WriteCallback callback) {
        final BulkRequest bulkRequest = new BulkRequest();
        final SimpleDateFormat formatter = new SimpleDateFormat(INDEX_NAME_DATE_PATTERN);

        try {
            for (final AlertWithId alertWrapper : alerts) {
                final String idxName = indexName(formatter, alertWrapper.getAlert().getCreationTime());
                final IndexRequest indexRequest = new IndexRequest(idxName, ES_INDEX_TYPE, alertWrapper.getId());
                indexRequest.source(convertAlertToMap(alertWrapper.getAlert()));
                bulkRequest.add(indexRequest);
                this.client.bulkAsync(bulkRequest, new BulkActionListener(bulkRequest, callback, 0));
            }
        } catch (IOException ex) {
            callback.onComplete(ex);
        }
    }

    final class BulkActionListener implements ActionListener<BulkResponse> {

        private final WriteCallback callback;
        private final int retryCount;
        private final BulkRequest bulkRequest;

        BulkActionListener(final BulkRequest bulkRequest, final WriteCallback callback, int retryCount) {
            this.callback = callback;
            this.bulkRequest = bulkRequest;
            this.retryCount = retryCount;
        }

        @Override
        public void onResponse(BulkResponse bulkItemResponses) {
            if (bulkItemResponses.hasFailures()) {
                retry(new RuntimeException("Fail to execute the elastic search write with partial failures:"
                        + bulkItemResponses.buildFailureMessage()));
            } else {
                callback.onComplete(null);
            }
        }

        @Override
        public void onFailure(Exception e) {
            retry(e);
        }

        private void retry(final Exception e) {
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(retryBackOffMillis);
                    client.bulkAsync(bulkRequest, new BulkActionListener(bulkRequest, callback, retryCount + 1));
                } catch (InterruptedException e1) {
                    callback.onComplete(e);
                }
            } else {
                logger.error("All retries while writing to elastic search have been exhausted");
                callback.onComplete(e);
            }
        }
        // visible for testing
        public int getRetryCount() {
            return retryCount;
        }
    }

    private XContentBuilder convertAlertToMap(final Alert alert) throws IOException {
        return XContentFactory.jsonBuilder()
                .startObject()
                .field(NAME, alert.getName())
                .field(CREATION_TIME, alert.getCreationTime())
                .field(ANNOTATIONS, alert.getAnnotations())
                .field(LABELS, alert.getLabels())
                .field(GENERATOR_URL, alert.getGeneratorURL())
                .endObject();
    }

    private String indexName(final SimpleDateFormat formatter, final long startTime) {
        return String.format("%s-%s", indexNamePrefix, formatter.format(new Date(startTime)));
    }
}
