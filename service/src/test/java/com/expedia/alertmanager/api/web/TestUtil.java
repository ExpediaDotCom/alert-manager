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
package com.expedia.alertmanager.api.web;

import com.expedia.alertmanager.model.Dispatcher;
import com.expedia.alertmanager.model.Field;
import com.expedia.alertmanager.model.Operand;
import com.expedia.alertmanager.model.User;

public class TestUtil {

    public static Operand operand(String key, String value) {
        Operand operand = new Operand();
        Field field = new Field();
        field.setKey(key);
        field.setValue(value);
        operand.setField(field);
        return operand;
    }

    public static Dispatcher dispatcher(String endpoint, Dispatcher.Type type) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setEndpoint(endpoint);
        dispatcher.setType(type);
        return dispatcher;
    }

    public static User user(String id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
