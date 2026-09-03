package com.fizzycoyotestudio.eventforge.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
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

    /**
     * Identifies the browser/visitor this session belongs to (see the
     * "ef_player" cookie in GamePlayController) so "My Games" can list
     * "your" sessions without requiring a real login. Nullable because
     * sessions created directly via the REST API (GameController) have
     * no associated web player, and so existing rows predating this
     * column stay valid without a data migration.
     */
    private UUID playerId;

    @Column(nullable = false)
    private String currentEventBusinessId;

    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String stateJson;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean triggered;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean terminal;

    /**
     * How many events have fired in this session so far — the "clock"
     * that Event#cooldownTicks is measured against. Incremented once per
     * actually-triggered event (see GameSessionPersistenceService).
     * Defaults to 0 for old rows predating this column, which is exactly
     * the right starting value.
     */
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int currentTick;

    /**
     * JSON map of eventBusinessId -> the tick it last fired on, used to
     * evaluate cooldowns when picking from a weighted nextEventPool.
     * Null/absent (old rows) is treated as "nothing on cooldown yet" by
     * CooldownJsonMapper.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String cooldownJson;

    /** Nullable for the same reason as playerId — old rows just won't have one. */
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
