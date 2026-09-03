package com.fizzycoyotestudio.eventforge.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @JdbcTypeCode(SqlTypes.JSON)
    private String stateJson;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean triggered;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean terminal;
}
