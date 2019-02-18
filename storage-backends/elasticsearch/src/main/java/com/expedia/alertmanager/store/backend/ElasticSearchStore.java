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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.expedia.alertmanager.model.store.AlertStore;
import com.expedia.alertmanager.model.store.AlertWithId;
import com.google.common.base.Supplier;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import vc.inreach.aws.request.AWSSigner;
import vc.inreach.aws.request.AWSSigningRequestInterceptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ElasticSearchStore implements AlertStore {
    final static String ES_INDEX_TYPE = "alerts";
    public static final String SERVICE_NAME = "es";

    static String NAME = "name";
    static String CREATION_TIME = "startTime";
    static String ANNOTATIONS = "annotations";
    static String LABELS = "labels";
    static String EXPECTED_VALUE = "expectedValue";
    static String OBSERVED_VALUE = "observedValue";
    static String GENERATOR_URL = "generatorURL";

    private final static String DEFAULT_INDEX_PREFIX = "alerts";

    private RestHighLevelClient client;
    private Reader reader;
    private Writer writer;

    @Override
    public void read(final Map<String, String> labels,
                     final long from,
                     final long to,
                     final int size,
                     final ReadCallback callback) {
        reader.read(labels, from, to, size, callback);
    }

    @Override
    public void write(final List<AlertWithId> alerts, final WriteCallback callback) {
        writer.write(alerts, callback);
    }

    @Override
    public void init(final Map<String, Object> config) throws IOException {
        log.info("Initializing elastic search store with config {}", config);
        val indexNamePrefix = config.getOrDefault("index.prefix", DEFAULT_INDEX_PREFIX).toString();
        val esHost = config.getOrDefault("host", "http://localhost:9200").toString();
        val clientBuilder = RestClient.builder(HttpHost.create(esHost));
        addAWSRequestSignerInterceptorIfRequired(clientBuilder, config);
        //FIXME -  we need to align to one library for all AM components unless there is a valid reason to use multiple.
        //here we are using RestHighLevelClient but in service module, we are using Jest client.
        this.client = new RestHighLevelClient(clientBuilder);
        this.reader = new Reader(client, config, indexNamePrefix, log);
        this.writer = new Writer(client, config, indexNamePrefix, log);
        applyIndexTemplate(config);
    }

    private void addAWSRequestSignerInterceptorIfRequired(RestClientBuilder clientBuilder, Map<String, Object> config) {
        val needsIAMAuth = Boolean.valueOf(config.getOrDefault("es.aws-iam-auth-required",
            "false").toString());
        if (needsIAMAuth) {
            val awsRegion = config.get("es.aws_region").toString();
            Optional<AWSSigningRequestInterceptor> signingInterceptor = getAWSRequestSignerInterceptor(awsRegion);
            signingInterceptor.ifPresent(
                interceptor -> clientBuilder.setHttpClientConfigCallback(
                    clientConf -> clientConf.addInterceptorLast(interceptor)));
        }
    }

    private Optional<AWSSigningRequestInterceptor> getAWSRequestSignerInterceptor(String awsRegion) {
        final Supplier<LocalDateTime> clock = () -> LocalDateTime.now(ZoneOffset.UTC);
        AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
        val awsSigner = new AWSSigner(credentialsProvider, awsRegion, SERVICE_NAME, clock);
        return Optional.of(new AWSSigningRequestInterceptor(awsSigner));
    }

    private void applyIndexTemplate(final Map<String, Object> config) throws IOException {
        Object template = config.get("template");
        if (template == null) {
            // read from resource
            val reader = new BufferedReader(
                    new InputStreamReader(
                            this.getClass().getResourceAsStream("/index_template.json")));
            template = reader.lines().collect(Collectors.joining("\n"));
            reader.close();
        }

        if (!template.toString().isEmpty()) {
            log.info("Applying indexing template {}", template);
            val entity = new NStringEntity(template.toString(), ContentType.APPLICATION_JSON);
            val resp = this.client.getLowLevelClient()
                    .performRequest("PUT", "/_template/alert-store-template", new HashMap<>(), entity);

            if (resp.getStatusLine() == null ||
                    (resp.getStatusLine().getStatusCode() < 200 && resp.getStatusLine().getStatusCode() >= 300)) {
                throw new IOException(String.format("Fail to execute put template request '%s'", template.toString()));
            } else {
                log.info("indexing template has been successfully applied - '{}'", template);
            }
        }
    }

    @Override
    public void close() {
        /* close the client quietly */
        try {
            client.close();
        } catch (Exception e) {
            log.error("Fail to close elastic client with error", e);
        }
    }
}
