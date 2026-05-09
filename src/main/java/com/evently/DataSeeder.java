package com.evently;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.evently.model.Event;
import com.evently.model.EventStatus;
import com.evently.model.User;
import com.evently.repository.EventRepository;
import com.evently.repository.UserRepository;

/**
 * DataSeeder runs once at startup and populates the database with:
 *   1. A default admin user (if none exists yet).
 *   2. Five sample published events with future dates (if no events exist yet).
 *
 * The seeder is idempotent — safe to run on every startup without creating duplicates.
 *
 * Default admin credentials:
 *   email:    admin@evently.com
 *   password: Admin@1234
 *
 * OWNER: Faris
 */
@Component
@SuppressWarnings("null")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository  userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      EventRepository eventRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedSuperAdmin();
        seedAdminUser();
        seedSampleEvents();
    }

    // ------------------------------------------------------------------
    // Super admin — Faris (sole super admin; promotes/demotes other admins)
    // ------------------------------------------------------------------
    private void seedSuperAdmin() {
        if (userRepository.existsByEmail("Farisnabil075@gmail.com")) {
            log.info("DataSeeder: super admin (Faris) already exists — skipping.");
            return;
        }

        User superAdmin = User.builder()
                .name("Faris")
                .email("Farisnabil075@gmail.com")
                .password(passwordEncoder.encode("admin@123"))
                .isAdmin(true)
                .isSuperAdmin(true)
                .currencyPreference("USD")
                .build();

        userRepository.save(superAdmin);
        log.info("DataSeeder: super admin created (Farisnabil075@gmail.com).");
    }

    // ------------------------------------------------------------------
    // Default admin user
    // ------------------------------------------------------------------
    private void seedAdminUser() {
        if (userRepository.existsByEmail("admin@evently.com")) {
            log.info("DataSeeder: admin user already exists — skipping.");
            return;
        }

        User admin = User.builder()
                .name("Admin")
                .email("admin@evently.com")
                .password(passwordEncoder.encode("Admin@1234"))
                .isAdmin(true)
                .currencyPreference("USD")
                .build();

        userRepository.save(admin);
        log.info("DataSeeder: default admin user created (admin@evently.com).");
    }

    // ------------------------------------------------------------------
    // Sample events
    // ------------------------------------------------------------------
    private void seedSampleEvents() {
        if (eventRepository.count() > 0) {
            log.info("DataSeeder: events already exist — skipping event seed.");
            return;
        }

        List<Event> sampleEvents = List.of(
            Event.builder()
                .title("Spring Tech Conference 2026")
                .description("A full-day conference covering the latest in Spring Boot, microservices, and cloud-native Java. Talks, workshops, and networking sessions.")
                .dateTime(LocalDateTime.now().plusDays(30))
                .location("Cairo International Conference Centre, Cairo")
                .capacity(200)
                .price(new BigDecimal("150.00"))
                .status(EventStatus.PUBLISHED)
                .build(),

            Event.builder()
                .title("Web Development Bootcamp")
                .description("Intensive 2-day hands-on bootcamp covering HTML, CSS, JavaScript, and modern frameworks. Bring your laptop!")
                .dateTime(LocalDateTime.now().plusDays(14))
                .location("Tech Hub Alexandria, Alexandria")
                .capacity(50)
                .price(new BigDecimal("75.00"))
                .status(EventStatus.PUBLISHED)
                .build(),

            Event.builder()
                .title("Open Source Community Meetup")
                .description("Monthly meetup for open-source contributors. Lightning talks, project showcase, and free pizza.")
                .dateTime(LocalDateTime.now().plusDays(7))
                .location("GrEEK Campus, Cairo")
                .capacity(100)
                .price(null) // free
                .status(EventStatus.PUBLISHED)
                .build(),

            Event.builder()
                .title("AI & Machine Learning Workshop")
                .description("Hands-on workshop exploring Python, scikit-learn, and practical ML applications. No prior ML experience required.")
                .dateTime(LocalDateTime.now().plusDays(45))
                .location("Smart Village, Giza")
                .capacity(30)
                .price(new BigDecimal("200.00"))
                .status(EventStatus.PUBLISHED)
                .build(),

            Event.builder()
                .title("Cybersecurity Awareness Day")
                .description("Free public event covering OWASP Top 10, phishing awareness, and best practices for developers and end-users alike.")
                .dateTime(LocalDateTime.now().plusDays(60))
                .location("Cairo University, Faculty of Computers")
                .capacity(300)
                .price(null) // free
                .status(EventStatus.PUBLISHED)
                .build()
        );

        eventRepository.saveAll(sampleEvents);
        log.info("DataSeeder: {} sample events created.", sampleEvents.size());
    }
}
