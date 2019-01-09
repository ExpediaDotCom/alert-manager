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
package com.expedia.alertmanager.service.model;

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
    public static final String AM_PREFIX = "am_";
    public static final String NAME = AM_PREFIX + "name";
    public static final String USER_KEYWORD = AM_PREFIX + "user";
    public static final String USER_ID_KEYWORD = "id";
    public static final String DISPATCHERS_KEYWORD = AM_PREFIX + "dispatchers";
    public static final String QUERY_KEYWORD = AM_PREFIX + "query";
    public static final String LAST_MOD_TIME_KEYWORD = AM_PREFIX + "lastModifiedTime";
    public static final String CREATE_TIME_KEYWORD = AM_PREFIX + "createdTime";

    @SerializedName(NAME)
    private String name;
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
