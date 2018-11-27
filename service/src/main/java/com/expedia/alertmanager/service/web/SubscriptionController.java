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
package com.expedia.alertmanager.service.web;

import com.expedia.alertmanager.service.dao.SubscriptionStoreService;
import com.expedia.alertmanager.model.CreateSubscriptionRequest;
import com.expedia.alertmanager.model.SearchSubscriptionRequest;
import com.expedia.alertmanager.model.SubscriptionResponse;
import com.expedia.alertmanager.model.UpdateSubscriptionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SubscriptionController {

    @Autowired
    private SubscriptionStoreService subscriptionStore;

    @Autowired
    private RequestValidator requestValidator;

    @RequestMapping(value = "/subscriptions", method = RequestMethod.POST)
    public List<String> createSubscriptions(@RequestBody List<CreateSubscriptionRequest> createSubRqs) {
        Assert.notEmpty(createSubRqs, "CreateSubscriptionRequests should not be empty");
        createSubRqs.forEach(createSubRq -> {
            requestValidator.validateExpression(createSubRq.getExpression());
            requestValidator.validateDispatcher(createSubRq.getDispatchers());
            requestValidator.validateUser(createSubRq.getUser());
        });

        return subscriptionStore.createSubscriptions(createSubRqs);
    }

    @RequestMapping(value = "/subscriptions/search", method = RequestMethod.POST)
    public List<SubscriptionResponse> searchSubscriptions(
        @RequestBody SearchSubscriptionRequest searchSubscriptionRequest) {
        Assert.isTrue(!StringUtils.isEmpty(searchSubscriptionRequest.getUserId())
                || !ObjectUtils.isEmpty(searchSubscriptionRequest.getLabels()),
            "user id or labels needs to be present");
        Assert.isTrue(!(!StringUtils.isEmpty(searchSubscriptionRequest.getUserId())
                && !ObjectUtils.isEmpty(searchSubscriptionRequest.getLabels())),
            "search by both user id and labels not supported");

        return subscriptionStore.searchSubscriptions(searchSubscriptionRequest.getUserId(),
            searchSubscriptionRequest.getLabels());
    }

    @RequestMapping(value = "/subscriptions/{id}", method = RequestMethod.GET)
    public SubscriptionResponse getSubscription(@PathVariable String id) {
        return subscriptionStore.getSubscription(id);
    }

    @RequestMapping(value = "/subscriptions", method = RequestMethod.PUT)
    public ResponseEntity updateSubscriptions(@RequestBody List<UpdateSubscriptionRequest> updateSubRqs) {
        Assert.notEmpty(updateSubRqs, "UpdateSubscriptionRequests should not be empty");
        updateSubRqs.forEach(updateSubRq -> {
            requestValidator.validateExpression(updateSubRq.getExpression());
            requestValidator.validateDispatcher(updateSubRq.getDispatchers());
        });

        subscriptionStore.updateSubscriptions(updateSubRqs);
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/subscriptions/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteSubscription(@PathVariable String id) {
        Assert.notNull(id, "id can't be null");
        subscriptionStore.deleteSubscription(id);
        return new ResponseEntity(HttpStatus.OK);
    }
}
