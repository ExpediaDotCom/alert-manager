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
package com.expedia.alertmanager.util;

import com.expedia.alertmanager.notifier.PagerDutyMessage;
import com.expedia.alertmanager.temp.MappedMetricData;
import com.expedia.metrics.MetricDefinition;
import com.google.gson.Gson;

import java.time.format.DateTimeFormatter;
import java.util.Map;

public class PagerDutyJsonBuilder extends JsonBuilder<String> {

    private final Gson mapper = new Gson();

    private final String pdKey;

    public PagerDutyJsonBuilder(String pdKey) {
        this.pdKey = pdKey;
    }

    @Override
    public String build(MappedMetricData mappedMetricData) {
        return mapper.toJson(buildPagerDutyMessage(mappedMetricData));
    }

    //TODO - Move this to a template
    private PagerDutyMessage buildPagerDutyMessage(MappedMetricData mappedMetricData) {
        Map<String, String> tags = decodeTags(mappedMetricData);
        PagerDutyMessage.Payload payload = PagerDutyMessage.Payload.builder()
            .summary(getSummary(mappedMetricData.getMetricData().getMetricDefinition()))
            .source("AdaptiveAlerting")
            .timestamp(dateFromEpochSeconds(mappedMetricData.getMetricData().getTimestamp(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .severity("info")
            .tags(tags)
            .build();
        return PagerDutyMessage.builder()
            .routingKey(this.pdKey)
            .eventAction("trigger")
            .payload(payload).build();
    }

    private String getSummary(MetricDefinition metricDefinition) {
        return "[FIRING] " + metricDefinition.getKey();
    }
}
