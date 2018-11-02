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
package com.expedia.alertmanager.dao;

import com.expedia.alertmanager.entity.Subscription;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SubscriptionRepository extends CrudRepository<Subscription, Long> {

    public List<Subscription> findByDetectorIdAndMetricId(String detectorId, String metricId);

    public List<Subscription> findByDetectorIdAndOwner(String detectorId, String owner);

    public List<Subscription> findByDetectorId(String detectorId);

    public List<Subscription> findByOwner(String owner);
}
