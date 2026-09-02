package com.fizzycoyotestudio.eventforge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSessionEntity, UUID> {
}
