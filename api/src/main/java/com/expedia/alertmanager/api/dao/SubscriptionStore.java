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
package com.expedia.alertmanager.api.dao;

import com.expedia.alertmanager.api.conf.ElasticSearchConfig;
import com.expedia.alertmanager.api.model.SubscriptionEntity;
import com.expedia.alertmanager.api.util.QueryUtil;
import com.expedia.alertmanager.model.CreateSubscriptionRequest;
import com.expedia.alertmanager.model.ExpressionTree;
import com.expedia.alertmanager.model.SubscriptionResponse;
import com.expedia.alertmanager.model.UpdateSubscriptionRequest;
import com.google.gson.Gson;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Delete;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Get;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.indices.mapping.GetMapping;
import io.searchbox.indices.mapping.PutMapping;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.percolator.PercolateQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SubscriptionStore {

    private final Gson GSON = new Gson();

    @Autowired
    private JestClientFactory clientFactory;

    @Autowired
    private ElasticSearchConfig elasticSearchConfig;

    @Autowired
    private QueryUtil queryUtil;

    public List<String> createSubscriptions(List<CreateSubscriptionRequest> createSubRqs) {
        Set<String> fields = getFields(createSubRqs);
        Set<String> missingFieldMappings = getMissingFieldsMappings(fields);
        //add new fields to index mappings
        updateIndexMappings(missingFieldMappings);

        //create subscriptions details
        return storeSubscriptions(createSubRqs);
    }

    private List<String> storeSubscriptions(List<CreateSubscriptionRequest> createSubRqs) {
        Bulk.Builder bulkIndexBuilder = new Bulk.Builder();
        for (CreateSubscriptionRequest rq : createSubRqs) {
            SubscriptionEntity subscriptionEntity = new SubscriptionEntity(rq.getUser(), rq.getDispatchers(),
                queryUtil.buildQuery(rq.getExpression()));
            bulkIndexBuilder.addAction(new Index.Builder(subscriptionEntity)
                .index(elasticSearchConfig.getIndexName()).type(elasticSearchConfig.getDocType()).build());
        }
        JestClient client = clientFactory.getObject();
        try {
            JestResult result = client.execute(bulkIndexBuilder.build());
            validateResponseStatus(result);
            return ((BulkResult) result).getItems().stream()
                .map(item -> item.id).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Store subscriptions failed", e);
            throw new RuntimeException(e);
        }
    }

    private Set<String> getFields(List<CreateSubscriptionRequest> createSubRqs) {
        Set<String> fields = new HashSet<>();
        createSubRqs.forEach(createSubRq -> {
            fields.addAll(getFieldsPerRequest(createSubRq.getExpression()));
        });
        return fields;
    }

    private Set<String> getFieldsPerRequest(ExpressionTree expression) {
        return expression.getOperands().stream()
            .map(operand -> operand.getField().getKey()).collect(Collectors.toSet());
    }

    public void updateSubscriptions(List<UpdateSubscriptionRequest> updateSubscriptionRequests) {
        Bulk.Builder bulkIndexBuilder = new Bulk.Builder();
        for (UpdateSubscriptionRequest rq : updateSubscriptionRequests) {
            SubscriptionResponse existingSubscription = getSubscription(rq.getId());
            SubscriptionEntity subscriptionEntity = new SubscriptionEntity(existingSubscription.getUser(),
                rq.getDispatchers(),
                queryUtil.buildQuery(rq.getExpression()));
            bulkIndexBuilder.addAction(new Index.Builder(subscriptionEntity).index(elasticSearchConfig.getIndexName())
                .type(elasticSearchConfig.getDocType()).id(rq.getId()).build());
        }
        JestClient client = clientFactory.getObject();
        try {
            JestResult result = client.execute(bulkIndexBuilder.build());
            validateResponseStatus(result);
        } catch (IOException e) {
            log.error("Update subscriptions failed", e);
            throw new RuntimeException(e);
        }
    }

    private void validateResponseStatus(JestResult result) {
        if (!result.isSucceeded()) {
            log.error("Response failed with message: " + result.getErrorMessage());
            throw new RuntimeException(result.getErrorMessage());
        }
    }

    private void updateIndexMappings(Set<String> missingFieldMappings) {
        JestClient client = clientFactory.getObject();
        try {
            if (!missingFieldMappings.isEmpty()) {
                Map<String, Object> message = new HashMap<>();
                message.put("type", "keyword");
                Map<String, Object> properties = new HashMap<>();
                missingFieldMappings.forEach(field -> {
                    properties.put(field, message);
                });
                Map<String, Object> jsonMap = new HashMap<>();
                jsonMap.put("properties", properties);
                PutMapping builder = new PutMapping.Builder(elasticSearchConfig.getIndexName(),
                    elasticSearchConfig.getDocType(), jsonMap).build();
                JestResult result = client.execute(builder);
                validateResponseStatus(result);
            }
        } catch (IOException e) {
            log.error("Update index mappings failed", e);
            throw new RuntimeException(e);
        }
    }

    private Set<String> getMissingFieldsMappings(Set<String> fields) {
        JestClient client = clientFactory.getObject();
        try {
            GetMapping getMapping = new GetMapping.Builder()
                .addIndex(elasticSearchConfig.getIndexName())
                .addType(elasticSearchConfig.getDocType())
                .build();
            JestResult result = client.execute(getMapping);
            validateResponseStatus(result);
            Set<String> mappedFields = result.getJsonObject().get(elasticSearchConfig.getIndexName())
                .getAsJsonObject().get("mappings")
                .getAsJsonObject().get(elasticSearchConfig.getDocType()).getAsJsonObject()
                .get("properties").getAsJsonObject()
                .entrySet().stream().map(en -> en.getKey()).collect(Collectors.toSet());
            fields.removeAll(mappedFields);
        } catch (IOException e) {
            log.error("Get index mappings failed", e);
            throw new RuntimeException(e);
        }
        return fields;
    }

    public List<SubscriptionResponse> searchSubscriptions(String user, Map<String, String> labels) {
        JestClient client = clientFactory.getObject();
        try {
            if (user != null) {
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(
                    QueryBuilders.nestedQuery("user",
                    QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("user.id", user)), ScoreMode.None));
                return getSubscriptionResponses(client, searchSourceBuilder);
            } else {
                XContentBuilder xContent = XContentFactory.jsonBuilder();
                xContent.map(labels);
                PercolateQueryBuilder percolateQuery =
                    new PercolateQueryBuilder("query", elasticSearchConfig.getDocType(), xContent.bytes(),
                        XContentType.JSON);
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(percolateQuery);
                long startTime = System.currentTimeMillis();
                List<SubscriptionResponse> responses = getSubscriptionResponses(client, searchSourceBuilder);
                long stopTime = System.currentTimeMillis();
                log.info("Search elapsed time:{}", stopTime - startTime);
                return responses;
            }
        } catch (IOException e) {
            log.error("Search subscriptions failed", e);
            throw new RuntimeException(e);
        }
    }

    public SubscriptionResponse getSubscription(String id) {
        JestClient client = clientFactory.getObject();
        Get get = new Get.Builder(elasticSearchConfig.getIndexName(), id)
            .type(elasticSearchConfig.getDocType()).build();
        try {
            DocumentResult result = client.execute(get);
            validateResponseStatus(result);
            return getSubscriptionResponse(result.getSourceAsString(), result.getId());
        } catch (IOException e) {
            log.error("Get subscription with id " + id + " failed", e);
            throw new RuntimeException(e);
        }
    }

    public void deleteSubscription(String id) {
        JestClient client = clientFactory.getObject();
        try {
            DocumentResult result = client.execute(new Delete.Builder(id)
                .index(elasticSearchConfig.getIndexName())
                .type(elasticSearchConfig.getDocType())
                .id(id)
                .build());
            validateResponseStatus(result);
        } catch (IOException e) {
            log.error("Delete subscription with id " + id + " failed", e);
            throw new RuntimeException(e);
        }
    }

    private SubscriptionResponse getSubscriptionResponse(String json, String id) {
        SubscriptionEntity subscriptionEntity = GSON.fromJson(json, SubscriptionEntity.class);
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(id);
        response.setDispatchers(subscriptionEntity.getDispatchers());
        response.setExpression(queryUtil.buildExpressionTree(subscriptionEntity.getQuery()));
        response.setUser(subscriptionEntity.getUser());
        return response;
    }

    private List<SubscriptionResponse> getSubscriptionResponses(JestClient client,
                                                                SearchSourceBuilder searchSourceBuilder)
        throws IOException {
        JestResult result = client.execute(new Search.Builder(searchSourceBuilder.toString())
            .addIndex(elasticSearchConfig.getIndexName()).addType(elasticSearchConfig.getDocType()).build());
        validateResponseStatus(result);
        List<SearchResult.Hit<Object, Void>> hits = ((SearchResult) result).getHits(Object.class);
        return hits.stream()
            .map(hit -> getSubscriptionResponse(GSON.toJson(hit.source), hit.id))
            .collect(Collectors.toList());
    }

}
