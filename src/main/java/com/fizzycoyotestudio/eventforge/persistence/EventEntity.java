package com.fizzycoyotestudio.eventforge.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(columnNames = {"scenario_id", "business_id"}))
@Getter
@Setter
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Matches the domain Event.id (e.g. "zombie-attack"), unique within its scenario. */
    @Column(name = "business_id", nullable = false)
    private String businessId;

    private String name;
    private String description;

    @ManyToOne
    @JoinColumn(name = "scenario_id")
    private ScenarioEntity scenario;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String conditionJson;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String actionsJson;

    /** Business id of the next Event, or null if this is a terminal event. Not a DB FK on purpose — see ADR note below. */
    private String nextEventBusinessId;

    /** How many ticks must pass after this event fires before it's eligible again as a random-pool candidate. 0 = no cooldown. */
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int cooldownTicks;

    /** JSON list of WeightedTransition — the weighted candidates for automatic random transition, if any. */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String nextEventPoolJson;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChoiceEntity> choices = new ArrayList<>();
}
