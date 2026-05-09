package com.evently.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evently.model.Session;

/**
 * Data access for the {@code sessions} table.
 *
 * OWNER: Alei
 */
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySessionToken(String sessionToken);

    void deleteBySessionToken(String sessionToken);

    List<Session> findByUserIdOrderByLastActivityDesc(Long userId);
}
