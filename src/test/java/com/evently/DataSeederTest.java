package com.evently;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.evently.model.Event;
import com.evently.model.EventStatus;
import com.evently.model.User;
import com.evently.repository.EventRepository;
import com.evently.repository.UserRepository;

/**
 * Integration tests for the DataSeeder against H2.
 *
 * Verifies idempotency, admin user creation, and sample event seeding.
 *
 * Uses @DataJpaTest + @Import to load only the JPA layer plus DataSeeder and
 * its dependency.
 *
 * OWNER: Faris
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({DataSeeder.class, BCryptPasswordEncoder.class})
class DataSeederTest {

    @Autowired
    private DataSeeder dataSeeder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;

    /**
     * BCryptPasswordEncoder is injected into DataSeeder via @Import. We also
     * need it here to verify the encoded password.
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Start each test with a clean database to test seeding from scratch.
        eventRepository.deleteAll();
        userRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // Admin user seed
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("run() creates admin user when none exists")
    void run_createsAdminUser_whenNoneExists() throws Exception {
        dataSeeder.run();

        java.util.Optional<User> admin = userRepository.findByEmail("admin@evently.com");
        assertThat(admin).isPresent();
        assertThat(admin.get().isAdmin()).isTrue();
        assertThat(admin.get().getName()).isEqualTo("Admin");
        assertThat(admin.get().getCurrencyPreference()).isEqualTo("USD");
    }

    @Test
    @DisplayName("run() encodes admin password with BCrypt")
    void run_encodesAdminPassword() throws Exception {
        dataSeeder.run();

        User admin = userRepository.findByEmail("admin@evently.com").orElseThrow();
        // The stored password must be a BCrypt hash, NOT plain text.
        assertThat(admin.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("Admin@1234", admin.getPassword())).isTrue();
    }

    @Test
    @DisplayName("run() is idempotent — calling twice does not create duplicate admin")
    void run_idempotent_doesNotDuplicateAdmin() throws Exception {
        dataSeeder.run();
        dataSeeder.run(); // second call must be a no-op

        long adminCount = userRepository.findAll().stream()
                .filter(user -> "admin@evently.com".equals(user.getEmail()))
                .count();
        assertThat(adminCount).isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // Sample events seed
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("run() creates exactly 5 sample events on first run")
    void run_createsFiveSampleEvents() throws Exception {
        dataSeeder.run();

        long count = eventRepository.count();
        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("All seeded events have PUBLISHED status")
    void run_allSeedEventsArePublished() throws Exception {
        dataSeeder.run();

        List<Event> events = eventRepository.findAll();
        assertThat(events).allMatch(e -> e.getStatus() == EventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("All seeded events have future dateTimes")
    void run_allSeedEventsHaveFutureDates() throws Exception {
        dataSeeder.run();

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Event> events = eventRepository.findAll();
        assertThat(events).allMatch(e -> e.getDateTime().isAfter(now));
    }

    @Test
    @DisplayName("run() is idempotent — calling twice does not duplicate sample events")
    void run_idempotent_doesNotDuplicateEvents() throws Exception {
        dataSeeder.run();
        dataSeeder.run(); // second call should be skipped (count > 0 check)

        long count = eventRepository.count();
        assertThat(count).isEqualTo(5);
    }
}
