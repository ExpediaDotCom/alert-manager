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
package com.expedia.alertmanager.api.web;

import com.expedia.alertmanager.api.dao.SubscriptionStore;
import com.expedia.alertmanager.model.CreateSubscriptionRequest;
import com.expedia.alertmanager.model.Dispatcher;
import com.expedia.alertmanager.model.ExpressionTree;
import com.expedia.alertmanager.model.Field;
import com.expedia.alertmanager.model.Operand;
import com.expedia.alertmanager.model.Operator;
import com.expedia.alertmanager.model.User;
import com.google.gson.Gson;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(SubscriptionController.class)
public class SubscriptionControllerTests {

    private final Gson GSON = new Gson();

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SubscriptionStore subscriptionStore;

    @MockBean
    private RequestValidator requestValidator;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void givenAnEmptyCreateSubscriptionRequests_shouldFailValidation() throws Exception {
        exceptionRule.expect(NestedServletException.class);
        exceptionRule.expectMessage("Request processing failed; " +
            "nested exception is java.lang.IllegalArgumentException: CreateSubscriptionRequests should not be empty");
        mvc.perform(post("/subscriptions")
            .content("[]")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void givenValidCreateSubscriptionRequests_shouldCreateSubscriptions() throws Exception {
        User user = new User();
        user.setId("xyz");
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setEndpoint("email");
        dispatcher.setType(Dispatcher.Type.EMAIL);
        Field field = new Field();
        field.setKey("app");
        field.setValue("search-app");
        Operand operand = new Operand();
        operand.setField(field);
        ExpressionTree expression = new ExpressionTree();
        expression.setOperator(Operator.AND);
        expression.setOperands(Arrays.asList(operand));
        CreateSubscriptionRequest createSubscriptionRequest = new CreateSubscriptionRequest();
        createSubscriptionRequest.setUser(user);
        List<Dispatcher> dispatchers = Arrays.asList(dispatcher);
        createSubscriptionRequest.setDispatchers(dispatchers);
        createSubscriptionRequest.setExpression(expression);

        List<CreateSubscriptionRequest> createSubReqs = Arrays.asList(createSubscriptionRequest);
        given(subscriptionStore.createSubscriptions(createSubReqs)).willReturn(Arrays.asList("123"));
        mvc.perform(post("/subscriptions")
            .content(GSON.toJson(createSubReqs))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0]").value("123"));
        verify(requestValidator, times(1)).validateUser(user);
        verify(requestValidator, times(1)).validateExpression(expression);
        verify(requestValidator, times(1)).validateDispatcher(dispatchers);
        verify(subscriptionStore, times(1)).createSubscriptions(createSubReqs);
    }

    //TODO - Need to add more test cases.
}
