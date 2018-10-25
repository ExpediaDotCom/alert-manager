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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

@Service
public class MailContentBuilder {
    private final TemplateEngine templateEngine;

    @Autowired
    public MailContentBuilder(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String build(MappedMetricData mappedMetricData) {
        Context context = new Context();
        context.setVariable("metricKey", mappedMetricData.getMetricData().getMetricDefinition().getKey());
        context.setVariable("metricValue", mappedMetricData.getMetricData().getValue());
        context.setVariable("detector", mappedMetricData.getDetectorType());
        context.setVariable("metadata", createMetaData(mappedMetricData));
        return templateEngine.process("emailTemplate", context);
    }

    private Map<String, String> createMetaData(MappedMetricData mappedMetricData) {
        Map<String, String> metadata = new HashMap<>(mappedMetricData.getMetricData().getMetricDefinition()
            .getTags().getKv());
        metadata.put("anomalyLevel", mappedMetricData.getAnomalyResult().getAnomalyLevel().name());
        return metadata;
    }
}
