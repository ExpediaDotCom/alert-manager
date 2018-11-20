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

import com.expedia.alertmanager.model.ExpressionTree;
import com.expedia.alertmanager.model.User;
import org.junit.Test;

import java.util.Collections;

public class RequestValidatorTests {

    private RequestValidator requestValidator = new RequestValidator();

    @Test(expected = IllegalArgumentException.class)
    public void givenNullUser_validateUserShouldFail() {
        requestValidator.validateUser(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyUser_validateUserShouldFail() {
        requestValidator.validateUser(new User());
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyDispatcherList_validateDispatchersShouldFail() {
        requestValidator.validateDispatcher(Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullExpression_validateExpressionShouldFail() {
        requestValidator.validateExpression(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyExpression_validateExpressionShouldFail() {
        requestValidator.validateExpression(new ExpressionTree());
    }

    //TODO - need to add more extensive test cases
}
