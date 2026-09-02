package com.fizzycoyotestudio.eventforge.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "game_sessions")
@Getter
@Setter
public class GameSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID scenarioId;

    @Column(nullable = false)
    private String currentEventBusinessId;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String stateJson;
}
