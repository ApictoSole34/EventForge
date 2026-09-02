package com.fizzycoyotestudio.eventforge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScenarioRepository extends JpaRepository<ScenarioEntity, UUID> {
    Optional<ScenarioEntity> findByName(String name);
}