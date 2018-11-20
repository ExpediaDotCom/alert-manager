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

package com.expedia.alertmanager.store.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Accessors @Getter
public class KafkaConfig {
    @JsonProperty("stream.threads")
    private int streamThreads = 2;

    private String topic = "alerts";

    @JsonProperty("max.wakeups")
    private int maxWakeups = 10;

    @JsonProperty("wakeup.timeout.ms")
    private long wakeupTimeoutInMillis = 3000;

    @JsonProperty("max.commit.retries")
    private int maxCommitRetries = 3;

    @JsonProperty("commit.backoff.ms")
    private int commitBackOffMillis = 200;

    @JsonProperty("commit.interval.ms")
    private int commitIntervalMillis = 3000;

    @JsonProperty("close.timeout.ms")
    private long closeTimeoutMillis = 1000;

    @JsonProperty("poll.timeout.ms")
    private long pollTimeoutMillis = 2000;

    // this consumer map can contain any property that is understood by kafka consumer like group.id, bootstrap.servers
    private Map<Object, Object> consumer = new HashMap<>();
}
