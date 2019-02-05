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
package com.expedia.alertmanager.service.web;

import com.expedia.alertmanager.model.Dispatcher;
import com.expedia.alertmanager.model.ExpressionTree;
import com.expedia.alertmanager.model.Operator;
import com.expedia.alertmanager.model.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.expedia.alertmanager.service.model.SubscriptionEntity.AM_PREFIX;
import static com.expedia.alertmanager.service.model.SubscriptionEntity.CREATE_TIME_KEYWORD;
import static com.expedia.alertmanager.service.model.SubscriptionEntity.DISPATCHERS_KEYWORD;
import static com.expedia.alertmanager.service.model.SubscriptionEntity.QUERY_KEYWORD;
import static com.expedia.alertmanager.service.model.SubscriptionEntity.USER_KEYWORD;
import static com.expedia.alertmanager.service.web.TestUtil.operand;

public class RequestValidatorTests {

    //Rule needs to be public
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private RequestValidator requestValidator = new RequestValidator();

    @Before
    public void before() {
        ReflectionTestUtils.setField(requestValidator, "additionalEmailValidator", Optional.empty());
    }

    @Test
    public void givenNullUser_validateUserShouldFail() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("subscription user can't null");
        requestValidator.validateUser(null);
    }

    @Test
    public void givenEmptyUser_validateUserShouldFail() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("subscription userId can't empty");
        requestValidator.validateUser(new User());
    }

    @Test
    public void givenUserWithUserIdHavingWhiteSpace_validateUserShouldFail() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("subscription userId can't contain whitespaces");
        User user  = new User();
        user.setId("id id");
        requestValidator.validateUser(user);
    }

    @Test
    public void givenEmptyDispatcherList_validateDispatchersShouldFail() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("subscription dispatchers can't empty");
        requestValidator.validateDispatcher(Collections.emptyList());
    }

    @Test
    public void givenInvalidEmailInEmailDispatcher_validateDispatchersShouldFail() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("invalid email 'abc'");
        Dispatcher emailDispatcher = new Dispatcher();
        emailDispatcher.setEndpoint("email@email.com, abc");
        emailDispatcher.setType(Dispatcher.Type.EMAIL);
        requestValidator.validateDispatcher(Collections.singletonList(emailDispatcher));
    }

    @Test
    public void givenUnExpectedEmailInEmailDispatcher_validateDispatchersShouldFail() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("email 'email@email.com' doesn't confirm to the required pattern");
        Dispatcher emailDispatcher = new Dispatcher();
        emailDispatcher.setEndpoint("email@email.com");
        emailDispatcher.setType(Dispatcher.Type.EMAIL);
        ReflectionTestUtils.setField(requestValidator, "additionalEmailValidator",
            Optional.of(Pattern.compile("^[A-Z0-9._%+-]+@domain.com$", Pattern.CASE_INSENSITIVE)));

        requestValidator.validateDispatcher(Collections.singletonList(emailDispatcher));
    }

    @Test
    public void givenNullExpression_validateExpressionShouldFail() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("subscription expression can't null");
        requestValidator.validateExpression(null);
    }

    @Test
    public void givenAnExpressionWith_OR_Operator_validateExpressionShouldFail() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Only AND operator is supported now");
        ExpressionTree expressionTree = new ExpressionTree();
        expressionTree.setOperator(Operator.OR);
        requestValidator.validateExpression(expressionTree);
    }

    @Test
    public void givenAnExpressionWithEmptyOperands_validateExpressionShouldFail() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Operands can't be empty");
        ExpressionTree expressionTree = new ExpressionTree();
        expressionTree.setOperator(Operator.AND);
        requestValidator.validateExpression(expressionTree);
    }

    @Test
    public void givenAnExpressionWithOperandsHavingReserved_QueryKeyword_validateExpressionShouldFail() {
        assertExpressionOperandName(QUERY_KEYWORD);
    }

    @Test
    public void givenAnExpressionWithOperandsHavingReserved_UserKeyword_validateExpressionShouldFail() {
        assertExpressionOperandName(USER_KEYWORD);
    }

    @Test
    public void givenAnExpressionWithOperandsHavingReserved_CreatedTimeKeyword_validateExpressionShouldFail() {
        assertExpressionOperandName(CREATE_TIME_KEYWORD);
    }

    @Test
    public void givenAnExpressionWithOperandsHavingReserved_DispatcherKeyword_validateExpressionShouldFail() {
        assertExpressionOperandName(DISPATCHERS_KEYWORD);
    }

    @Test
    public void givenAnExpressionWithOperandsHavingReserved_Prefix_validateExpressionShouldFail() {
        thrown.expect(IllegalArgumentException.class);
        ExpressionTree expressionTree = new ExpressionTree();
        expressionTree.setOperator(Operator.AND);
        expressionTree.setOperands(Arrays.asList(operand(AM_PREFIX + "xx", "test")));
        requestValidator.validateExpression(expressionTree);
    }

    private void assertExpressionOperandName(String operandName) {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(String.format("Invalid operand field key '%s'. am_ is a reserved prefix",
            operandName));
        ExpressionTree expressionTree = new ExpressionTree();
        expressionTree.setOperator(Operator.AND);
        expressionTree.setOperands(Arrays.asList(operand(operandName, "test")));
        requestValidator.validateExpression(expressionTree);
    }
}
