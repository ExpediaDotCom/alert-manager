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

import com.expedia.alertmanager.temp.MappedMetricData;
import com.expedia.metrics.MetricDefinition;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class JsonBuilder<T> {

    public abstract T build(MappedMetricData mappedMetricData);

    protected Map<String, String> decodeTags(MappedMetricData mappedMetricData) {
        MetricDefinition metricDefinition = mappedMetricData.getMetricData().getMetricDefinition();
        Map<String, String> tags = metricDefinition.getTags().getKv();
        tags.put("model", mappedMetricData.getDetectorType());
        tags.put("value", String.valueOf(mappedMetricData.getMetricData().getValue()));
        return tags;
    }

    protected String dateFromEpochSeconds(long seconds, DateTimeFormatter formatter) {
        ZonedDateTime zdt = getZonedDateTimeInUtc(seconds);
        return formatter.format(zdt);
    }

    private ZonedDateTime getZonedDateTimeInUtc(long seconds) {
        Instant instant = Instant.ofEpochSecond(seconds);
        return ZonedDateTime.ofInstant(
            instant, ZoneId.systemDefault()).withZoneSameLocal(ZoneId.of("Etc/UTC"));
    }
}