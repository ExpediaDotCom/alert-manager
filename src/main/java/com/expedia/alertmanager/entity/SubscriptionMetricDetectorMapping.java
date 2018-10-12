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
package com.expedia.alertmanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import java.sql.Timestamp;
@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Entity(name = "subscription_metric_detector_mapping")
public class SubscriptionMetricDetectorMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_id")
    private String metricId;

    @Column(name = "detector_id")
    private String detectorId;

    @OneToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "date_created")
    private Timestamp timestamp;

    public SubscriptionMetricDetectorMapping(String metricId, String detectorId, Subscription subscription) {
        this.metricId = metricId;
        this.detectorId = detectorId;
        this.subscription = subscription;
    }
}
