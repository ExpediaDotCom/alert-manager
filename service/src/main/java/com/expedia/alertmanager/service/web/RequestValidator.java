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
import com.expedia.alertmanager.model.Operand;
import com.expedia.alertmanager.model.Operator;
import com.expedia.alertmanager.model.User;
import com.expedia.alertmanager.model.util.EmailDispatcherHelper;
import com.expedia.alertmanager.service.conf.AppConfig;
import com.expedia.alertmanager.service.model.SubscriptionEntity;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class RequestValidator {

    @Autowired
    private AppConfig appConfig;

    private Optional<Pattern> additionalEmailValidator;

    @PostConstruct
    public void init() {
        additionalEmailValidator
            = StringUtils.isEmpty(appConfig.getAdditionalEmailValidatorExp()) ? Optional.empty() :
                    Optional.of(Pattern.compile(appConfig.getAdditionalEmailValidatorExp(), Pattern.CASE_INSENSITIVE));
    }

    public void validateUser(User user) {
        Assert.notNull(user, "subscription user can't null");
        Assert.isTrue(!StringUtils.isEmpty(user.getId()), "subscription userId can't empty");
        Assert.isTrue(!StringUtils.containsWhitespace(user.getId()), "subscription userId can't " +
            "contain whitespaces");
    }

    public void validateDispatcher(List<Dispatcher> dispatchers) {
        Assert.notEmpty(dispatchers, "subscription dispatchers can't empty");
        dispatchers.forEach(dispatcher -> {
            Assert.notNull(dispatcher.getEndpoint(), "dispatcher endpoint can't be null");
            if (dispatcher.getType() == Dispatcher.Type.EMAIL) {
                EmailDispatcherHelper.getToEmails(dispatcher).forEach(email -> {
                    Assert.isTrue(EmailValidator.getInstance().isValid(email),
                        String.format("invalid email '%s'", email));
                    additionalEmailValidator.ifPresent(validator -> {
                        Assert.isTrue(validator.matcher(email).find(),
                            String.format("email '%s' doesn't confirm to the required pattern", email));
                    });
                });
            }
        });
    }

    public void validateExpression(ExpressionTree expression) {
        Assert.notNull(expression, "subscription expression can't null");
        //Only AND operator is supported now
        Assert.isTrue(Operator.AND.equals(expression.getOperator()), "Only AND operator is supported now");
        Assert.notEmpty(expression.getOperands(), "Operands can't be empty");
        expression.getOperands().forEach(operand -> {
            validateOperand(operand);
        });
    }

    private void validateOperand(Operand operand) {
        //Nested conditions are not supported now
        Assert.isNull(operand.getExpression(), "Nested expressions are not supported");
        Assert.notNull(operand.getField(), "Operands can't be empty");
        Assert.isTrue(!StringUtils.isEmpty(operand.getField().getKey()), "Invalid operand field key");
        Assert.isTrue(!StringUtils.isEmpty(operand.getField().getValue()), "Invalid operand field value");
        Assert.isTrue(!operand.getField().getKey().startsWith(SubscriptionEntity.AM_PREFIX),
            String.format("Invalid operand field key '%s'. %s is a reserved prefix",
                operand.getField().getKey(), SubscriptionEntity.AM_PREFIX));
    }
}
