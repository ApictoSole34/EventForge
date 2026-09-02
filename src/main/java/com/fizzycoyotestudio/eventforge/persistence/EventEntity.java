package com.fizzycoyotestudio.eventforge.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    private String conditionJson;

    @Column(columnDefinition = "jsonb")
    private String actionsJson;

    /** Business id of the next Event, or null if this is a terminal event. Not a DB FK on purpose — see ADR note below. */
    private String nextEventBusinessId;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChoiceEntity> choices = new ArrayList<>();
}
