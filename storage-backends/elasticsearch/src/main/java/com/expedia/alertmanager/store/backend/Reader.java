package com.expedia.alertmanager.store.backend;

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.model.store.AlertWithId;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.expedia.alertmanager.store.backend.ElasticSearchStore.*;

class Reader {
    private final static int DEFAULT_MAX_READ_SIZE = 10000;
    private final static long DEFAULT_READ_TIMEOUT_MS = 15000;

    private final RestHighLevelClient client;
    private final String indexNamePrefix;
    private final Logger logger;
    private final TimeValue readTimeout;
    private final int maxSize;

    Reader(final RestHighLevelClient client,
           final Map<String, Object> config,
           final String indexNamePrefix,
           final Logger logger) {
        this.client = client;
        this.indexNamePrefix = indexNamePrefix;
        this.logger = logger;
        this.readTimeout = readTimeout(config);
        this.maxSize = maxReadSize(config);
    }

    void read(final Map<String, String> labels,
              final long from,
              final long to,
              final int size,
              final ReadCallback callback) {
        final SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        final BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        labels.forEach((key, value) -> boolQuery.must(QueryBuilders.matchQuery(LABELS + '.' + key, value)));
        boolQuery.must(new RangeQueryBuilder(CREATION_TIME).gt(from).lt(to));

        sourceBuilder
                .query(boolQuery)
                .timeout(readTimeout)
                .size(size == 0 ? maxSize : size);

        final SearchRequest searchRequest =
                new SearchRequest()
                        .source(sourceBuilder)
                        .indices(this.indexNamePrefix + "*");

        this.client.searchAsync(searchRequest, new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(final SearchResponse searchResponse) {
                final List<AlertWithId> alerts = new ArrayList<>();
                for (final SearchHit hit : searchResponse.getHits().getHits()) {
                    final AlertWithId aId = new AlertWithId();
                    aId.setId(hit.getId());
                    if (hit.getSourceAsMap() != null) {
                        aId.setAlert(convertMapToAlertData(hit.getSourceAsMap()));
                    }
                    alerts.add(aId);
                }
                callback.onComplete(alerts, Optional.empty());
            }

            @Override
            public void onFailure(final Exception ex) {
                logger.error("Fail to read the alert response from elastic search", ex);
                callback.onComplete(new ArrayList<>(), Optional.of(ex));
            }
        });
    }

    private static Alert convertMapToAlertData(final Map<String, Object> sourceAsMap) {
        final Alert alert = new Alert();
        alert.setCreationTime(Long.parseLong(sourceAsMap.get(CREATION_TIME).toString()));
        alert.setName(sourceAsMap.get(NAME).toString());
        alert.setLabels((Map<String, String>)sourceAsMap.get(LABELS));
        if (sourceAsMap.get(ANNOTATIONS) != null) {
            alert.setAnnotations((Map<String, String>) sourceAsMap.get(ANNOTATIONS));
        }
        if (sourceAsMap.get(GENERATOR_URL) != null) {
            alert.setGeneratorURL(sourceAsMap.get(GENERATOR_URL).toString());
        }
        return alert;
    }

    private int maxReadSize(Map<String, Object> config) {
        return Integer.parseInt(config.getOrDefault("max.read.size", DEFAULT_MAX_READ_SIZE).toString());
    }

    private TimeValue readTimeout(final Map<String, Object> config) {
        final long timeout = Long.parseLong(config.getOrDefault("read.timeout.ms", DEFAULT_READ_TIMEOUT_MS).toString());
        return new TimeValue(timeout, TimeUnit.MILLISECONDS);
    }
}
