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
package com.expedia.alertmanager.api.util;

import com.expedia.alertmanager.api.model.BoolCondition;
import com.expedia.alertmanager.api.model.MustCondition;
import com.expedia.alertmanager.api.model.Query;
import com.expedia.alertmanager.model.ExpressionTree;
import com.expedia.alertmanager.model.Field;
import com.expedia.alertmanager.model.Operand;
import com.expedia.alertmanager.model.Operator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QueryUtil {

    public ExpressionTree buildExpressionTree(Query query) {
        ExpressionTree expressionTree = new ExpressionTree();
        //TODO - derive operator from query. for now hardcoding to AND as this is the only operator supported now.
        expressionTree.setOperator(Operator.AND);
        List<Operand> operands = query.getBool().getMust().stream()
            .map(mustCondition -> {
                Operand operand = new Operand();
                Field field = mustCondition.getMatch().entrySet().stream()
                    .map(match -> new Field(match.getKey(), match.getValue()))
                    .collect(Collectors.toList()).get(0);
                operand.setField(field);
                return operand;
            }).collect(Collectors.toList());
        expressionTree.setOperands(operands);
        return expressionTree;
    }

    public Query buildQuery(ExpressionTree expressionTree) {
        List<MustCondition> mustConditions = expressionTree.getOperands().stream()
            .map(operand -> {
                Map<String, String> condition = new HashMap<>();
                condition.put(operand.getField().getKey(), operand.getField().getValue());
                return new MustCondition(condition);
            })
            .collect(Collectors.toList());
        BoolCondition boolCondition = new BoolCondition(mustConditions);
        return new Query(boolCondition);
    }
}
