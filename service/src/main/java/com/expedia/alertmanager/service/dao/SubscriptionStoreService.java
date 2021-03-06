/*
 * Copyright 2018-2019 Expedia Group, Inc.
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
package com.expedia.alertmanager.service.dao;

import com.expedia.alertmanager.model.BaseSubscription;
import com.expedia.alertmanager.model.CreateSubscriptionRequest;
import com.expedia.alertmanager.model.ExpressionTree;
import com.expedia.alertmanager.model.SearchSubscriptionRequest;
import com.expedia.alertmanager.model.SubscriptionResponse;
import com.expedia.alertmanager.model.UpdateSubscriptionRequest;
import com.expedia.alertmanager.service.conf.ElasticSearchConfig;
import com.expedia.alertmanager.service.model.SubscriptionEntity;
import com.expedia.alertmanager.service.util.QueryUtil;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SubscriptionStoreService {

    private final Gson GSON = new Gson();

    @Autowired
    private JestClientFactory clientFactory;

    @Autowired
    private ElasticSearchConfig elasticSearchConfig;

    @Autowired
    private QueryUtil queryUtil;

    public List<String> createSubscriptions(List<CreateSubscriptionRequest> createSubRqs) {
        /*
            Fields/Conditions in the expression are dynamic.
            We want the mapping type of each field to be 'keyword' inorder to find exact match
            when a search is performed.
            So the idea is to find all the new fields for which there is no existing mapping types
            and then create the field mappings for them with type as 'keyword' before a subscription is stored in
            elastic search.
         */
        Set<String> fields = getFields(createSubRqs);
        Set<String> newFieldMappings = getFieldsWithoutExistingMapping(fields);
        updateIndexMappings(newFieldMappings);

        //create subscriptions
        return storeSubscriptions(createSubRqs);
    }

    private List<String> storeSubscriptions(List<CreateSubscriptionRequest> createSubRqs) {
        Bulk.Builder bulkIndexBuilder = new Bulk.Builder();
        for (CreateSubscriptionRequest rq : createSubRqs) {
            Index indexReq = buildCreateSubscriptionRequest(rq);
            bulkIndexBuilder.addAction(indexReq);
        }
        JestClient client = clientFactory.getObject();
        try {
            BulkResult result = client.execute(bulkIndexBuilder.build());
            validateResponseStatus(result);
            return result.getItems().stream()
                .map(item -> item.id).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Store subscriptions failed", e);
            throw new RuntimeException(e);
        } finally {
            closeConnection(client);
        }
    }

    private Index buildCreateSubscriptionRequest(CreateSubscriptionRequest createSubRq) {
        long now = Instant.now().toEpochMilli();
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity(createSubRq.getName(),
            createSubRq.getUser(),
            createSubRq.getDispatchers(),
            queryUtil.buildQuery(createSubRq.getExpression()), now, now);
        return new Index.Builder(subscriptionEntity)
            .index(elasticSearchConfig.getIndexName()).type(elasticSearchConfig.getDocType()).build();
    }

    private Set<String> getFields(List<? extends BaseSubscription> subRqs) {
        Set<String> fields = new HashSet<>();
        subRqs.forEach(subRq -> {
            fields.addAll(getFieldsPerRequest(subRq.getExpression()));
        });
        return fields;
    }

    private Set<String> getFieldsPerRequest(ExpressionTree expression) {
        return expression.getOperands().stream()
            .map(operand -> operand.getField().getKey()).collect(Collectors.toSet());
    }

    public void updateSubscriptions(List<UpdateSubscriptionRequest> updateSubscriptionRequests) {
        Set<String> fields = getFields(updateSubscriptionRequests);
        Set<String> fieldsWithoutExistingMapping = getFieldsWithoutExistingMapping(fields);
        updateIndexMappings(fieldsWithoutExistingMapping);

        Bulk.Builder bulkIndexBuilder = new Bulk.Builder();
        for (UpdateSubscriptionRequest rq : updateSubscriptionRequests) {
            SubscriptionResponse existingSubscription = getSubscription(rq.getId());
            Index indexReq = buildUpdateSubscriptionRequest(rq, existingSubscription);
            bulkIndexBuilder.addAction(indexReq);
        }
        JestClient client = clientFactory.getObject();
        try {
            JestResult result = client.execute(bulkIndexBuilder.build());
            validateResponseStatus(result);
        } catch (IOException e) {
            log.error("Update subscriptions failed", e);
            throw new RuntimeException(e);
        } finally {
            closeConnection(client);
        }
    }

    private Index buildUpdateSubscriptionRequest(UpdateSubscriptionRequest updateSubscriptionReq,
                                                 SubscriptionResponse existingSubscription) {
        long now = Instant.now().toEpochMilli();
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity(
            updateSubscriptionReq.getName(),
            existingSubscription.getUser(),
            updateSubscriptionReq.getDispatchers(),
            queryUtil.buildQuery(updateSubscriptionReq.getExpression()), now, existingSubscription.getCreatedTime());
        return new Index.Builder(subscriptionEntity).index(elasticSearchConfig.getIndexName())
            .type(elasticSearchConfig.getDocType()).id(updateSubscriptionReq.getId()).build();
    }

    private void validateResponseStatus(JestResult result) {
        if (!result.isSucceeded()) {
            log.error("Response failed with message: " + result.getErrorMessage());
            throw new RuntimeException(result.getErrorMessage());
        }
    }

    private void updateIndexMappings(Set<String> newFieldMappings) {
        JestClient client = clientFactory.getObject();
        try {
            if (!newFieldMappings.isEmpty()) {
                PutMapping builder = buildUpdateMappingsRequest(newFieldMappings);
                JestResult result = client.execute(builder);
                validateResponseStatus(result);
            }
        } catch (IOException e) {
            log.error("Update index mappings failed", e);
            throw new RuntimeException(e);
        } finally {
            closeConnection(client);
        }
    }

    private PutMapping buildUpdateMappingsRequest(Set<String> missingFieldMappings) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "keyword");
        Map<String, Object> properties = new HashMap<>();
        missingFieldMappings.forEach(field -> {
            properties.put(field, message);
        });
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("properties", properties);
        return new PutMapping.Builder(elasticSearchConfig.getIndexName(),
            elasticSearchConfig.getDocType(), jsonMap).build();
    }

    private Set<String> getFieldsWithoutExistingMapping(Set<String> fields) {
        JestClient client = clientFactory.getObject();
        try {
            GetMapping getMapping = buildGetMappingRequest();
            JestResult result = client.execute(getMapping);
            validateResponseStatus(result);
            Set<String> mappedFields = result.getJsonObject().get(elasticSearchConfig.getIndexName())
                .getAsJsonObject().get("mappings")
                .getAsJsonObject().get(elasticSearchConfig.getDocType()).getAsJsonObject()
                .get("properties").getAsJsonObject()
                .entrySet().stream().map(en -> en.getKey()).collect(Collectors.toSet());
            fields.removeAll(mappedFields);
            return fields;
        } catch (IOException e) {
            log.error("Get index mappings failed", e);
            throw new RuntimeException(e);
        } finally {
            closeConnection(client);
        }
    }

    private GetMapping buildGetMappingRequest() {
        return new GetMapping.Builder()
                    .addIndex(elasticSearchConfig.getIndexName())
                    .addType(elasticSearchConfig.getDocType())
                    .build();
    }

    public List<SubscriptionResponse> matchSubscriptions(Map<String, String> labels) {
        JestClient client = clientFactory.getObject();
        try {
            XContentBuilder xContent = XContentFactory.jsonBuilder();
            xContent.map(labels);
            PercolateQueryBuilder percolateQuery =
                    new PercolateQueryBuilder(SubscriptionEntity.QUERY_KEYWORD, elasticSearchConfig.getDocType(), xContent.bytes(),
                            XContentType.JSON);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(percolateQuery);
            long startTime = System.currentTimeMillis();
            List<SubscriptionResponse> responses = getSubscriptionResponses(client, searchSourceBuilder);
            long stopTime = System.currentTimeMillis();
            log.info("Search elapsed time:{}", stopTime - startTime);
            return responses;
        } catch (IOException e) {
            log.error("Match subscriptions failed", e);
            throw new RuntimeException("Match subscriptions failed", e);
        } finally {
            closeConnection(client);
        }
    }

    public List<SubscriptionResponse> searchSubscriptions(SearchSubscriptionRequest searchRequest) {
        JestClient client = clientFactory.getObject();
        try {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(
                QueryBuilders.nestedQuery(SubscriptionEntity.USER_KEYWORD,
                QueryBuilders.boolQuery().must(QueryBuilders.matchQuery(
                    SubscriptionEntity.USER_KEYWORD + "." + SubscriptionEntity.USER_ID_KEYWORD,
                        searchRequest.getUserId())), ScoreMode.None));
            //FIXME setting default result set size to 200 until we have pagination.
            searchSourceBuilder.size(200);
            return getSubscriptionResponses(client, searchSourceBuilder);
        } catch (IOException e) {
            log.error("Search subscriptions failed", e);
            throw new RuntimeException("Search subscriptions failed", e);
        } finally {
            closeConnection(client);
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
        } finally {
            closeConnection(client);
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
        } finally {
            closeConnection(client);
        }
    }

    private SubscriptionResponse getSubscriptionResponse(String json, String id) {
        SubscriptionEntity subscriptionEntity = GSON.fromJson(json, SubscriptionEntity.class);
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(id);
        response.setName(subscriptionEntity.getName());
        response.setDispatchers(subscriptionEntity.getDispatchers());
        response.setExpression(queryUtil.buildExpressionTree(subscriptionEntity.getQuery()));
        response.setUser(subscriptionEntity.getUser());
        response.setLastModifiedTime(subscriptionEntity.getLastModifiedTime());
        response.setCreatedTime(subscriptionEntity.getCreatedTime());
        return response;
    }

    private List<SubscriptionResponse> getSubscriptionResponses(JestClient client,
                                                                SearchSourceBuilder searchSourceBuilder)
        throws IOException {
        SearchResult result = client.execute(new Search.Builder(searchSourceBuilder.toString())
            .addIndex(elasticSearchConfig.getIndexName()).addType(elasticSearchConfig.getDocType()).build());
        validateResponseStatus(result);
        List<SearchResult.Hit<Object, Void>> hits = result.getHits(Object.class);
        return hits.stream()
            .map(hit -> getSubscriptionResponse(GSON.toJson(hit.source), hit.id))
            .collect(Collectors.toList());
    }

    private void closeConnection(JestClient client) {
        try {
            client.close();
        } catch (IOException e) {
            log.error("Couldn't close the ES connection", e);
        }
    }

}
