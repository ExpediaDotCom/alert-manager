package com.expedia.alertmanager.model;

import lombok.Data;

@Data
public class Operand {
    private Field field;
    private ExpressionTree expression;
}
