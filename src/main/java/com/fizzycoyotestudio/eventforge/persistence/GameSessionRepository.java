package com.fizzycoyotestudio.eventforge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSessionEntity, UUID> {

    List<GameSessionEntity> findByPlayerIdOrderByUpdatedAtDesc(UUID playerId);

    Optional<GameSessionEntity> findFirstByPlayerIdAndScenarioIdAndTerminalFalseOrderByUpdatedAtDesc(
            UUID playerId, UUID scenarioId);

    void deleteByScenarioId(UUID scenarioId);
}