package com.expedia.alertmanager.api.web;

import com.expedia.alertmanager.model.SubscriptionResponse;
import com.expedia.alertmanager.model.CreateSubscriptionRequest;
import com.expedia.alertmanager.model.Dispatcher;
import com.expedia.alertmanager.model.ExpressionTree;
import com.expedia.alertmanager.model.UpdateSubscriptionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
public class SubscriptionController {

    @RequestMapping(value = "/api/subscriptions", method = RequestMethod.POST)
    public List<SubscriptionResponse> createSubscriptions(@RequestBody List<CreateSubscriptionRequest> createSubRqs) {
        Assert.notEmpty(createSubRqs, "CreateSubscriptionRequests should not be empty");
        createSubRqs.forEach(createSubRq -> {
            validateExpression(createSubRq.getExpression());
            validateDispatcher(createSubRq.getDispatchers());
            validateOwner(createSubRq);
        });

        //TODO - Store subscriptions

        return null;
    }

    private void validateOwner(CreateSubscriptionRequest createSubRq) {
        Assert.notNull(createSubRq.getOwner(), "subscription owner can't null");
    }

    private void validateDispatcher(List<Dispatcher> dispatchers) {
        Assert.notEmpty(dispatchers, "subscription dispatchers can't empty");
    }

    private void validateExpression(ExpressionTree expression) {
        Assert.notNull(expression, "subscription expression can't null");
    }

    @RequestMapping(value = "/api/subscriptions", method = RequestMethod.GET)
    public List<SubscriptionResponse> getSubscription(@RequestParam(required = false) String owner,
                                                      @RequestParam(required = false) Map<String, String> labels) {
        //TODO -  Fetch subscriptions

        return null;
    }

    @RequestMapping(value = "/api/subscriptions", method = RequestMethod.PUT)
    public List<SubscriptionResponse> updateSubscription(@RequestBody List<UpdateSubscriptionRequest> updateSubRqs) {
        Assert.notEmpty(updateSubRqs, "UpdateSubscriptionRequests should not be empty");
        updateSubRqs.forEach(updateSubRq -> {
            validateExpression(updateSubRq.getExpression());
            validateDispatcher(updateSubRq.getDispatchers());
        });

        //TODO - Update subscriptions

        return null;
    }

    @RequestMapping(value = "/api/subscriptions", method = RequestMethod.DELETE)
    public ResponseEntity deleteSubscription(@RequestParam String id) throws ExecutionException, InterruptedException {
        Assert.notNull(id, "id can't be null");

        //TODO - Delete subscription

        return null;
    }
}
