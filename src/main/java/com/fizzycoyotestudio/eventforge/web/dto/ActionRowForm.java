package com.fizzycoyotestudio.eventforge.web.dto;

import lombok.Data;

@Data
public class ActionRowForm {
    private String actionType = "MODIFY_RESOURCE";
    private String variable;
    private Double amount;
}
