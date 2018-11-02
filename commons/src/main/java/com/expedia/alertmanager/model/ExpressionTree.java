package com.expedia.alertmanager.model;

import lombok.Data;

import java.util.List;

@Data
public class ExpressionTree {
    private Operator operator;
    List<Operand> operands;
}
