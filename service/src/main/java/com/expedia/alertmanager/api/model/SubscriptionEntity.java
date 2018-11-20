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
package com.expedia.alertmanager.api.model;

import com.expedia.alertmanager.model.Dispatcher;
import com.expedia.alertmanager.model.User;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SubscriptionEntity {
    // Prefixing variable names with 'am_' to reserve these fields to be used in ES mappings.
    public static final String USER_KEYWORD = "am_user";
    public static final String USER_ID_KEYWORD = "id";
    public static final String DISPATCHERS_KEYWORD = "am_dispatchers";
    public static final String QUERY_KEYWORD = "am_query";
    public static final String LAST_MOD_TIME_KEYWORD = "am_lastModifiedTime";
    public static final String CREATE_TIME_KEYWORD = "am_createdTime";

    @SerializedName(USER_KEYWORD)
    private User user;
    @SerializedName(DISPATCHERS_KEYWORD)
    private List<Dispatcher> dispatchers;
    @SerializedName(QUERY_KEYWORD)
    private Query query;
    @SerializedName(LAST_MOD_TIME_KEYWORD)
    private long lastModifiedTime;
    @SerializedName(CREATE_TIME_KEYWORD)
    private long createdTime;
}
