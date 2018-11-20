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

import com.expedia.alertmanager.api.dao.AlertStore;
import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.model.SearchAlertsRequest;
import com.expedia.alertmanager.model.SearchAlertsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
public class AlertController {

    private final AlertStore alertStore;

    @Autowired
    public AlertController(AlertStore alertStore) {
        this.alertStore = alertStore;
    }

    @RequestMapping(value = "/alerts", method = RequestMethod.POST)
    public ResponseEntity saveAlerts(@RequestBody List<Alert> alerts) {
        alertStore.saveAlerts(alerts);
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/alerts/search", method = RequestMethod.POST)
    public CompletableFuture<SearchAlertsResponse> search(@RequestBody SearchAlertsRequest request) throws IOException {
        return alertStore
                .search(request.getLabels(), request.getFrom(), request.getTo(), request.getSize())
                .thenApply(alerts -> {
                    final SearchAlertsResponse response = new SearchAlertsResponse();
                    response.setAlerts(alerts);
                    return response;
                });
    }
}
