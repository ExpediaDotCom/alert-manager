/*
 * Copyright 2018 Expedia Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.expedia.alertmanager.store.backend;

import com.expedia.alertmanager.model.store.AlertWithId;
import com.expedia.alertmanager.model.store.ReadCallback;
import com.expedia.alertmanager.model.store.Store;
import com.expedia.alertmanager.model.store.WriteCallback;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ElasticSearchStore implements Store {
    final static String ES_INDEX_TYPE = "alerts";

    static String NAME = "name";
    static String START_TIME = "startTime";
    static String ANNOTATIONS = "annotations";
    static String LABELS = "labels";
    static String EXPECTED_VALUE = "expectedValue";
    static String OBSERVED_VALUE = "observedValue";
    static String GENERATOR_URL = "generatorURL";

    private final Logger logger = LoggerFactory.getLogger(ElasticSearchStore.class);
    private RestHighLevelClient client;
    private Reader reader;
    private Writer writer;

    @Override
    public void read(final Map<String, String> labels,
                     final long from,
                     final long to,
                     final int size,
                     final ReadCallback callback) throws IOException {
        reader.read(labels, from, to, size, callback);
    }

    @Override
    public void write(final List<AlertWithId> alerts, final WriteCallback callback) throws IOException {
        writer.write(alerts, callback);
    }

    @Override
    public void init(final Map<String, Object> config) throws IOException {
        logger.info("Initializing elastic search store with config {}", config);
        final String indexNamePrefix = config.getOrDefault("index.prefix", "alerts").toString();
        final String esHost = config.getOrDefault("hostname", "http://localhost:9092").toString();
        this.client = new RestHighLevelClient(RestClient.builder(HttpHost.create(esHost)));
        this.reader = new Reader(client, config, indexNamePrefix, logger);
        this.writer = new Writer(client, config, indexNamePrefix, logger);
        applyIndexTemplate(config);
    }

    private void applyIndexTemplate(final Map<String, Object> config) throws IOException {
        Object template = config.get("template");
        if (template == null) {
            // read from resource
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            this.getClass().getResourceAsStream("/index_template.json")));
            template = reader.lines().collect(Collectors.joining("\n"));
            reader.close();
        }



        final HttpEntity entity = new NStringEntity(template.toString(), ContentType.APPLICATION_JSON);
        final Response resp = this.client.getLowLevelClient()
                .performRequest("PUT", "/_template/alert-store-template", new HashMap<>(), entity);

        if (resp.getStatusLine() == null ||
                (resp.getStatusLine().getStatusCode() < 200 && resp.getStatusLine().getStatusCode() >= 300)) {
            throw new IOException(String.format("Fail to execute put template request '%s'", template.toString()));
        } else {
            logger.info("indexing template has been successfully applied - '{}'", template);
        }
    }

    @Override
    public void close() {
        /* close the client quietly */
        try {
            client.close();
        } catch (Exception e) {
            logger.error("Fail to close elastic client with error", e);
        }
    }
}
