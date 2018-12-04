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
package com.expedia.alertmanager.notifier.builder;

import com.expedia.alertmanager.model.Alert;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MessageComposer {

    @Autowired
    @Qualifier("freemarkerConfig")
    private Configuration freemarkerConfig;

    public String buildContent(Alert alert, String templateName) {
        try {
            Template template = freemarkerConfig.getTemplate(templateName);
            Map<String, Object> input = new HashMap();
            input.put( "alert", alert );
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, input);
        } catch (Exception e) {
            log.error("Exception generating content", e);
            throw new RuntimeException(e);
        }
    }


}
