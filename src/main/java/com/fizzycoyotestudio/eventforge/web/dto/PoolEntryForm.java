package com.fizzycoyotestudio.eventforge.web.dto;

import lombok.Data;

/** One row of a weighted random next-event pool, as authored in the HTML event form. */
@Data
public class PoolEntryForm {
    private String eventId;
    private Double weight;
}
