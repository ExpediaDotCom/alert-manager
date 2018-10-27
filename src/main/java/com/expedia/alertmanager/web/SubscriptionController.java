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
package com.expedia.alertmanager.web;

import com.expedia.alertmanager.dao.SubscriptionRepository;
import com.expedia.alertmanager.entity.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SubscriptionController {

    @Autowired
    private SubscriptionRepository subscriptionRepo;

    @ResponseStatus(value=HttpStatus.NOT_FOUND)
    @ExceptionHandler(IllegalArgumentException.class)
    public String notFound(Exception e) {
        return e.getMessage();
    }

    @RequestMapping(value = "/subscriptions", method = {RequestMethod.POST})
    public Iterable<Subscription> createSubscriptions(
        @RequestBody List<SubscriptionRequest> subscriptions) {
        Assert.notEmpty(subscriptions, "Input list can't be empty");
        List<Subscription> subscriptionInput =  new ArrayList<>();
        subscriptions.stream().map(sr -> {
            validateSubscriptionRequest(sr);
            return builder(sr).build();
        }).collect(Collectors.toList());
        return subscriptionRepo.saveAll(subscriptionInput);
    }

    private Subscription.SubscriptionBuilder builder(SubscriptionRequest sr) {
        return Subscription.builder()
            .metricId(sr.getMetricId())
            .detectorId(sr.getDetectorId())
            .name(sr.getName())
            .description(sr.getDescription())
            .type(sr.getType())
            .endpoint(sr.getEndpoint())
            .owner(sr.getOwner());
    }

    private void validateSubscriptionRequest(SubscriptionRequest sr) {
        Assert.noNullElements(new String[] {sr.getName(), sr.getDetectorId(), sr.getEndpoint(), sr.getType()},
            "name, detectorId, endpoint and type attributes can't be null");
        Assert.isTrue(Arrays.asList(Subscription.TYPE.values()).stream().anyMatch(type -> type.name().equals(sr.getType())),
            "type should be one of " + Arrays.asList(Subscription.TYPE.values()));
    }

    @RequestMapping(value = "/subscriptions", method = {RequestMethod.PUT})
    public Iterable<Subscription> updateSubscriptions(
        @RequestBody List<UpdateSubscriptionRequest> subscriptions) {
        Assert.notEmpty(subscriptions, "Input list can't be empty");
        List<Subscription> subscriptionInput = subscriptions.stream()
            .map(sur -> {
                validateUpdateSubscriptionRequest(sur);
                return builder(sur).id(sur.getId()).build();
        }).collect(Collectors.toList());
        return subscriptionRepo.saveAll(subscriptionInput);
    }

    private void validateUpdateSubscriptionRequest(UpdateSubscriptionRequest sur) {
        Assert.notNull(sur.getId(), "id can't be null");
        validateSubscriptionRequest(sur);
    }

    @RequestMapping(value = "/subscriptions/{id}", method = {RequestMethod.DELETE})
    public void deleteSubscription(@PathVariable long id) {
        Assert.notNull(id, "id can't be null");
        subscriptionRepo.findById(id).ifPresent(sub -> {
            subscriptionRepo.delete(sub);
        });
    }

    @RequestMapping(value = "/subscriptions", method = RequestMethod.GET)
    public List<Subscription> getSubscriptions(@RequestParam(required = false) String detectorId,
                                                                      @RequestParam(required = false) String metricId,
                                                                      @RequestParam(required = false) String owner) {
        if (detectorId != null && metricId != null) {
            return subscriptionRepo.findByDetectorIdAndMetricId(detectorId, metricId);
        }
        else if (detectorId != null) {
            return subscriptionRepo.findByDetectorId(detectorId);
        }
        else if (owner != null) {
            return subscriptionRepo.findByOwner(owner);
        }
        throw new IllegalArgumentException("Invalid Input Params");
    }

}
