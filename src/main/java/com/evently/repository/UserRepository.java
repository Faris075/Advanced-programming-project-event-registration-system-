package com.evently.repository;

import com.evently.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Data access for the `users` table.
 *
 * OWNER: Faris
 * USED BY: Alei (SecurityConfig, AuthController, ProfileController)
 *          Islam (AdminUserService)
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Lookup by email – used by UserDetailsService for login. */
    Optional<User> findByEmail(String email);

    /** Pre-registration uniqueness check before saving a new user. */
    boolean existsByEmail(String email);

    /** Admin user list – paginated. */
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
