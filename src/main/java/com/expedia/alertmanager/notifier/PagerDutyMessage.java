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
package com.expedia.alertmanager.notifier;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public class PagerDutyMessage {
    private final Payload payload;
    @SerializedName("routing_key")
    private final String routingKey;
    @SerializedName("dedupe_key")
    private final String dedupeKey;
    private final List<Image> images;
    private final List<Link> links;
    @SerializedName("event_action")
    private final String eventAction;
    private final String client;
    @SerializedName("client_url")
    private final String clientUrl;

    @Builder
    public static class Payload {
        private final String summary;
        private final String timestamp;
        private final String source;
        private final String severity;
        private final String component;
        private final String group;
        @SerializedName("class")
        private final String className;
        @SerializedName("custom_details")
        private final Map<String, String> tags;
    }

    static class Image {
        private String src;
        private String href;
        private String alt;
    }

    static class Link {
        private String href;
        private String text;
    }
}

