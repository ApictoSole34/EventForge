package com.fizzycoyotestudio.eventforge.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "choices")
@Getter
@Setter
public class ChoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String businessId;
    private String label;
    private String description;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String conditionJson;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String actionsJson;

    private String nextEventBusinessId;

    /** JSON list of WeightedTransition — the weighted candidates for random transition, if any. */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String nextEventPoolJson;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private EventEntity event;
}
