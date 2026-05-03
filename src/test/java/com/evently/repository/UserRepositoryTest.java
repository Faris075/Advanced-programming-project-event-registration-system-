package com.evently.repository;

import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;

import com.evently.model.User;

/**
 * Integration tests for UserRepository using the H2 in-memory database.
 *
 * @DataJpaTest spins up a minimal Spring context with JPA, Hibernate, and H2.
 * The evently_db MariaDB schema is not required for these tests.
 *
 * OWNER: Faris
 */
@DataJpaTest
@ActiveProfiles("test")   // loads application-test.properties if present (falls back to defaults)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    /** Clean slate before each test to prevent state leakage. */
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // findByEmail
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findByEmail returns user when email exists")
    void findByEmail_whenExists_returnsUser() {
        // Arrange
        User saved = userRepository.save(buildUser("alice@test.com", false));

        // Act
        Optional<User> result = userRepository.findByEmail("alice@test.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@test.com");
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findByEmail returns empty when email does not exist")
    void findByEmail_whenNotExists_returnsEmpty() {
        Optional<User> result = userRepository.findByEmail("nobody@nowhere.com");
        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // existsByEmail
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("existsByEmail returns true when email exists")
    void existsByEmail_whenExists_returnsTrue() {
        userRepository.save(buildUser("bob@test.com", false));
        assertThat(userRepository.existsByEmail("bob@test.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail returns false when email does not exist")
    void existsByEmail_whenNotExists_returnsFalse() {
        assertThat(userRepository.existsByEmail("ghost@test.com")).isFalse();
    }

    // -----------------------------------------------------------------------
    // findAllByOrderByCreatedAtDesc (pagination)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findAllByOrderByCreatedAtDesc returns all users paginated")
    void findAllPaginated_returnsUsers() {
        userRepository.save(buildUser("u1@test.com", false));
        userRepository.save(buildUser("u2@test.com", true));

        org.springframework.data.domain.Page<User> page =
                userRepository.findAllByOrderByCreatedAtDesc(
                        org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // save and isAdmin flag
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Admin user is persisted with isAdmin = true")
    void save_adminUser_persistsIsAdminTrue() {
        User admin = buildUser("admin@test.com", true);
        User saved = userRepository.save(admin);

        User loaded = userRepository.findById(Objects.requireNonNull(saved.getId())).orElseThrow();
        assertThat(loaded.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("Regular user is persisted with isAdmin = false")
    void save_regularUser_persistsIsAdminFalse() {
        User user = buildUser("user@test.com", false);
        User saved = userRepository.save(user);

        User loaded = userRepository.findById(Objects.requireNonNull(saved.getId())).orElseThrow();
        assertThat(loaded.isAdmin()).isFalse();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Builds a minimal valid User entity for testing.
     * The password value is a fake BCrypt-style string — no real encoding needed in unit tests.
     */
    private @NonNull User buildUser(String email, boolean isAdmin) {
        return Objects.requireNonNull(User.builder()
                .name("Test User")
                .email(email)
                .password("$2a$12$fakeHashForTestingOnly")
                .isAdmin(isAdmin)
                .currencyPreference("USD")
                .build());
    }
}
