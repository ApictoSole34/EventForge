package com.fizzycoyotestudio.eventforge.persistence;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scenarios")
@Getter
@Setter
public class ScenarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String description;

    @Column(columnDefinition = "jsonb")
    private String initialStateJson;

    /** Business id of the Event this scenario begins on. */
    private String startEventBusinessId;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventEntity> events = new ArrayList<>();
}
