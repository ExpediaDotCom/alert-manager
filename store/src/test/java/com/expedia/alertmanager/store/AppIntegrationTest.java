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

package com.expedia.alertmanager.store;

import com.expedia.alertmanager.model.Alert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class AppIntegrationTest {
    @Test
    public void testApp() throws Exception {
        final Path configFilePath = Paths.get("/tmp/application.yaml");
        final String config = "plugin.directory: \"storage-backends\"\n" +
                "plugin:\n" +
                "   name: \"elasticsearch\"\n" +
                "   jar.name: \"elasticsearch-store.jar\"\n" +
                "   conf:\n" +
                "    host: http://elasticsearch:9200\n" +
                "kafka:\n" +
                "  topic: alerts\n" +
                "  stream.threads: 2\n" +
                "  consumer:\n" +
                "    bootstrap.servers: kafkasvc:9092\n" +
                "    auto.offset.reset: earliest\n" +
                "    group.id: alert-manager-store\n" +
                "    enable.auto.commit: false\n" +
                "health.status.file: /tmp/health.status";

        Files.write(configFilePath, config.getBytes("utf-8"));

        produceAlertsInKakfa();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                App.main(new String[]{"/tmp/application.yaml"});
            } catch (Exception e) {
                Assert.fail("Fail to start the app with error message " + e.getMessage());
            }
        });

        Thread.sleep(15000);

        verifyElasticSearchData();

        Files.delete(configFilePath);
    }

    private void verifyElasticSearchData() throws IOException {
        final long currentTime = System.currentTimeMillis();
        final RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        HttpHost.create("http://elasticsearch:9200")));

        for (final String svc : Arrays.asList("svc1", "svc2")) {
            final SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            final BoolQueryBuilder boolQuery = QueryBuilders
                    .boolQuery()
                    .must(QueryBuilders.matchQuery("labels.service", svc));
            boolQuery.must(new RangeQueryBuilder("startTime").gt(currentTime - 90000).lt(currentTime));
            sourceBuilder
                    .query(boolQuery);

            final SearchRequest searchRequest =
                    new SearchRequest()
                            .source(sourceBuilder)
                            .indices("alerts*");

            final SearchResponse response = client.search(searchRequest);
            Assert.assertEquals(response.getHits().getHits().length, 1);
            final Map<String, Object> alert = response.getHits().getHits()[0].getSourceAsMap();
            Assert.assertEquals(alert.get("name"), "a1");
            Assert.assertEquals(alert.get("observedValue"), "5");
            Assert.assertEquals(alert.get("expectedValue"), "10");
            Assert.assertTrue("timestamp should be truncated to seconds",
                    Long.parseLong(alert.get("startTime").toString()) % 1000 == 0);
            Assert.assertEquals(((Map<String, String>) alert.get("labels")).get("service"), svc);
            Assert.assertEquals(((Map<String, String>) alert.get("annotations")).get("annotated_key"), "annotated_value");
        }
    }

    private void produceAlertsInKakfa() throws JsonProcessingException {
        final Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafkasvc:9092");
        final KafkaProducer<String, byte[]> producer = new KafkaProducer<>(props, new StringSerializer(), new ByteArraySerializer());

        final String svc1Alert = createAlert("svc1");
        final String svc2Alert = createAlert("svc2");

        Arrays.asList(svc1Alert, svc2Alert).forEach(alert -> {
            try {
                producer.send(new ProducerRecord<>("alerts", "k1", alert.getBytes("utf-8")), (recordMetadata, e) -> {
                    if (e != null) {
                        Assert.fail("Fail to produce the message to kafka with error message " + e.getMessage());
                    }
                });
            } catch (UnsupportedEncodingException e) {
                Assert.fail("Fail to produce the message to kafka with error message " + e.getMessage());
            }

        });

        producer.flush();
    }

    private String createAlert(final String serviceName) throws JsonProcessingException {
        final Alert alert = new Alert();
        alert.setName("a1");
        alert.setStartTime(System.currentTimeMillis());
        alert.setObservedValue("5");
        alert.setExpectedValue("10");

        final Map<String, String> labels = Collections.singletonMap("service", serviceName);
        alert.setLabels(labels);

        final Map<String, String> annotations = Collections.singletonMap("annotated_key", "annotated_value");
        alert.setAnnotations(annotations);

        return new ObjectMapper().writeValueAsString(alert);
    }
}
