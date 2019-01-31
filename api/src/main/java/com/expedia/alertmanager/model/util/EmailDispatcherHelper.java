/*
 * Copyright 2018-2019 Expedia Group, Inc.
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
package com.expedia.alertmanager.model.util;

import com.expedia.alertmanager.model.Dispatcher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EmailDispatcherHelper {
    private final static String EMAIL_DELIMITER = ",";

    public static List<String> getToEmails(Dispatcher dispatcher) {
        if (Dispatcher.Type.EMAIL != dispatcher.getType()) {
            throw new IllegalArgumentException("Dispatcher type is not EMAIL");
        }
        return Arrays.asList(dispatcher.getEndpoint().split(EMAIL_DELIMITER))
            .stream().map(email -> email.trim()).collect(Collectors.toList());
    }
}
