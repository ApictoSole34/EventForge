package com.fizzycoyotestudio.eventforge.web.dto;

import lombok.Data;

@Data
public class ConditionRowForm {
    private String variable;
    private String operator;
    private Double value;
    private boolean negate;
}
