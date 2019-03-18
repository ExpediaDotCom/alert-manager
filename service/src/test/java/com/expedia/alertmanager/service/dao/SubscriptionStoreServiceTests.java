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

import com.expedia.alertmanager.model.CreateSubscriptionRequest;
import com.expedia.alertmanager.model.Dispatcher;
import com.expedia.alertmanager.model.ExpressionTree;
import com.expedia.alertmanager.model.Operator;
import com.expedia.alertmanager.model.SubscriptionResponse;
import com.expedia.alertmanager.model.UpdateSubscriptionRequest;
import com.expedia.alertmanager.service.conf.ElasticSearchConfig;
import com.expedia.alertmanager.service.model.SubscriptionEntity;
import com.expedia.alertmanager.service.util.ElasticUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Get;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.indices.mapping.GetMapping;
import io.searchbox.indices.mapping.PutMapping;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.expedia.alertmanager.model.Dispatcher.Type.EMAIL;
import static com.expedia.alertmanager.service.web.TestUtil.dispatcher;
import static com.expedia.alertmanager.service.web.TestUtil.operand;
import static com.expedia.alertmanager.service.web.TestUtil.user;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(SpringRunner.class)
@SpringBootTest
public class SubscriptionStoreServiceTests {

    @Mock
    private JestClient client;

    @MockBean
    private JestClientFactory clientFactory;

    @MockBean
    private ElasticSearchConfig elasticSearchConfig;

    @Autowired
    private SubscriptionStoreService subscriptionStore;

    //TODO - remove this dependency if we can resolve bean loading issue with AlertStore in a better way
    @MockBean
    private AlertStoreService alertStore;

    @Test
    public void givenValidCreateSubscriptionRequest_shouldCreateSubscriptions() throws IOException {

        given(elasticSearchConfig.getIndexName()).willReturn("subscription");
        given(elasticSearchConfig.getDocType()).willReturn("_doc");
        given(client.execute(any(GetMapping.class))).willReturn(mockGetMappingResult());
        JestResult putMappingResult = new JestResult(new Gson());
        putMappingResult.setSucceeded(true);
        given(client.execute(any(PutMapping.class))).willReturn(putMappingResult);
        given(client.execute(any(Bulk.class))).willReturn(mockGetBulkIndexResult());
        given(clientFactory.getObject()).willReturn(client);

        ExpressionTree expression = new ExpressionTree();
        expression.setOperator(Operator.AND);
        expression.setOperands(Arrays.asList(operand("app", "search-app")));
        CreateSubscriptionRequest createSubscriptionRequest = new CreateSubscriptionRequest();
        createSubscriptionRequest.setName("sub");
        createSubscriptionRequest.setUser(user("user"));
        List<Dispatcher> dispatchers = Arrays.asList(dispatcher("email", EMAIL));
        createSubscriptionRequest.setDispatchers(dispatchers);
        createSubscriptionRequest.setExpression(expression);
        List<CreateSubscriptionRequest> createSubReqs = Arrays.asList(createSubscriptionRequest);
        List<String> ids = subscriptionStore.createSubscriptions(createSubReqs);
        assertEquals("id-123", ids.get(0));
        verify(client, times(1)).execute(any(GetMapping.class));
        verify(client, times(1)).execute(any(PutMapping.class));
        verify(client, times(1)).execute(any(Bulk.class));
    }

    @Test
    public void givenValidUpdateSubscriptionRequest_shouldUpdateSubscriptions() throws IOException {

        given(elasticSearchConfig.getIndexName()).willReturn("subscription");
        given(elasticSearchConfig.getDocType()).willReturn("_doc");

        given(client.execute(any(GetMapping.class))).willReturn(mockGetMappingResult());
        JestResult putMappingResult = new JestResult(new Gson());
        putMappingResult.setSucceeded(true);
        given(client.execute(any(PutMapping.class))).willReturn(putMappingResult);
        given(client.execute(any(Get.class))).willReturn(mockGetDocumentResult());
        given(client.execute(any(Bulk.class))).willReturn(mockGetBulkIndexResult());
        given(clientFactory.getObject()).willReturn(client);

        ExpressionTree expression = new ExpressionTree();
        expression.setOperator(Operator.AND);
        expression.setOperands(Arrays.asList(operand("app", "search-app")));
        UpdateSubscriptionRequest updateSubscriptionRequest = new UpdateSubscriptionRequest();
        updateSubscriptionRequest.setId("xyz");
        List<Dispatcher> dispatchers = Arrays.asList(dispatcher("email", EMAIL));
        updateSubscriptionRequest.setDispatchers(dispatchers);
        updateSubscriptionRequest.setExpression(expression);
        List<UpdateSubscriptionRequest> updateSubReqs = Arrays.asList(updateSubscriptionRequest);
        subscriptionStore.updateSubscriptions(updateSubReqs);
        verify(client, times(1)).execute(any(GetMapping.class));
        verify(client, times(1)).execute(any(PutMapping.class));
        verify(client, times(1)).execute(any(Get.class));
        verify(client, times(1)).execute(any(Bulk.class));
    }

    private BulkResult mockGetBulkIndexResult() {
        BulkResult bulkCreateResult = new BulkResult(new Gson());
        JsonObject item = new JsonObject();
        item.addProperty("_id", "id-123");
        item.addProperty("_index", "subscription");
        item.addProperty("_type", "doc");
        item.addProperty("status", "0");
        item.addProperty("_version", "111");
        JsonObject val = new JsonObject();
        val.add("item", item);
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(val);
        JsonObject items = new JsonObject();
        items.add("items", jsonArray);
        bulkCreateResult.setJsonMap(new Gson().fromJson(items, Map.class));
        bulkCreateResult.setSucceeded(true);
        return bulkCreateResult;
    }

    private DocumentResult mockGetDocumentResult() {
        DocumentResult getSubscriptionResult = new DocumentResult(new Gson());
        JsonObject source = getSourceJson();

        JsonObject sourceVal = new JsonObject();
        sourceVal.add("_source", source);
        getSubscriptionResult.setJsonMap(new Gson().fromJson(sourceVal, Map.class));
        getSubscriptionResult.setPathToResult("_source");
        getSubscriptionResult.setSucceeded(true);
        return getSubscriptionResult;
    }

    private JestResult mockGetMappingResult() {
        JestResult getMappingResult = new JestResult(new Gson());
        JsonObject settings = getIndexSettingsJson();
        JsonObject doc = new JsonObject();
        doc.addProperty("dynamic", "false");
        doc.add("properties", ElasticUtil.buildMappingsJson());
        doc.add("settings", settings);
        JsonObject mappings = new JsonObject();
        mappings.add("_doc", doc);
        JsonObject subscription = new JsonObject();
        subscription.add("aliases", new JsonObject());
        subscription.add("mappings", mappings);

        JsonObject subValue = new JsonObject();
        subValue.add("subscription", subscription);
        getMappingResult.setJsonMap(new Gson().fromJson(subValue, Map.class));
        getMappingResult.setPathToResult("_source");
        getMappingResult.setSucceeded(true);
        return getMappingResult;
    }

    private JsonObject getIndexSettingsJson() {
        JsonObject index = new JsonObject();
        index.addProperty("creation_date", "1542728536799");
        index.addProperty("number_of_shards", "5");
        index.addProperty("number_of_replicas", "1");
        index.addProperty("uuid", "j_Zte7uGTcq5qzZf4--h2w");
        index.addProperty("provided_name", "subscription");
        JsonObject settings = new JsonObject();
        settings.add("index", index);
        return settings;
    }

    @Test
    public void givenValidSearchSubscriptionRequest_shouldGetResults() throws IOException {

        given(elasticSearchConfig.getIndexName()).willReturn("subscription");
        given(elasticSearchConfig.getDocType()).willReturn("_doc");

        SearchResult searchResult = new SearchResult(new Gson());
        JsonObject source = getSourceJson();

        JsonObject sourceVal = new JsonObject();
        sourceVal.add("_source", source);
        sourceVal.addProperty("_id", "id-123");
        sourceVal.addProperty("_index", "subscription");
        sourceVal.addProperty("_type", "_doc");

        JsonArray sourceArray = new JsonArray();
        sourceArray.add(sourceVal);

        JsonObject sourcesVal = new JsonObject();
        sourcesVal.add("_source", sourceArray);
        //TODO - revisit if json meets the search result format
        searchResult.setJsonMap(new Gson().fromJson(
            sourcesVal, Map.class));
        searchResult.setSucceeded(true);
        searchResult.setPathToResult("_source");
        given(client.execute(any(Search.class))).willReturn(searchResult);
        given(clientFactory.getObject()).willReturn(client);

        ExpressionTree expression = new ExpressionTree();
        expression.setOperator(Operator.AND);
        expression.setOperands(Arrays.asList(operand("app", "search-app")));

        //search by user id
        List<SubscriptionResponse> subResponses = subscriptionStore.searchSubscriptions("user", null);
        assertEquals(subResponses.size(), 1);
        assertEquals(subResponses.get(0).getUser().getId(), "user");
        assertEquals(subResponses.get(0).getId(), "id-123");
        assertEquals(subResponses.get(0).getCreatedTime(), 1000);
        assertEquals(subResponses.get(0).getLastModifiedTime(), 10000);
    }

    private JsonObject getSourceJson() {
        JsonObject source = new JsonObject();
        source.addProperty(SubscriptionEntity.NAME, "sub");
        JsonObject user = new JsonObject();
        user.addProperty("id", "user");
        source.add(SubscriptionEntity.USER_KEYWORD, user);
        source.addProperty(SubscriptionEntity.LAST_MOD_TIME_KEYWORD, "10000");
        source.addProperty(SubscriptionEntity.CREATE_TIME_KEYWORD, "1000");
        JsonObject dispatcher = new JsonObject();
        dispatcher.addProperty("type", "EMAIL");
        dispatcher.addProperty("endpoint", "email@email.com");
        JsonArray dispatchers = new JsonArray();
        dispatchers.add(dispatcher);
        source.add(SubscriptionEntity.DISPATCHERS_KEYWORD, dispatchers);

        JsonObject keyVal = new JsonObject();
        keyVal.addProperty("app", "shop-app");
        JsonObject matchObj = new JsonObject();
        matchObj.add("match", keyVal);
        JsonArray matchArray = new JsonArray();
        matchArray.add(matchObj);
        JsonObject boolCond = new JsonObject();
        boolCond.add("must", matchArray);
        JsonObject query = new JsonObject();
        query.add("bool", boolCond);
        source.add(SubscriptionEntity.QUERY_KEYWORD, query);
        return source;
    }
}
