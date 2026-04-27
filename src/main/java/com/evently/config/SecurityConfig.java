package com.evently.config;

import com.evently.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Spring Security configuration.
 *
 * Defines:
 *   - Which URLs are public vs. require authentication.
 *   - Login and logout behaviour.
 *   - Remember-me token management.
 *   - UserDetailsService that loads User entities from the database.
 *
 * OWNER: Alei
 *
 * TODO (Alei):
 *   1. Complete the securityFilterChain bean below.
 *   2. Configure form login (login page, success URL, failure URL).
 *   3. Configure logout (logout URL, success URL, clear cookies).
 *   4. Configure remember-me with the APP_SECRET key and userDetailsService.
 *   5. Wire in the UserDetailsService lambda (already sketched below).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${evently.security.remember-me-key}")
    private String rememberMeKey;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Loads a User entity by email and wraps it in Spring Security's UserDetails.
     *
     * Grants ROLE_ADMIN to admin users, ROLE_USER to everyone else.
     * This bean is used by the login form, remember-me, and session management.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            com.evently.model.User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));

            String role = user.isAdmin() ? "ROLE_ADMIN" : "ROLE_USER";

            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    List.of(new SimpleGrantedAuthority(role))
            );
        };
    }

    /**
     * TODO (Alei): Implement the security filter chain.
     *
     * Required authorisation rules:
     *   - Public (no auth):  GET /, /events/**, /auth/**, /css/**, /js/**
     *   - Authenticated:     /dashboard/**, /register/**, /profile/**
     *   - Admin only:        /admin/**  (also guarded by AdminInterceptor)
     *
     * Form login:
     *   - Login page:    GET  /auth/login
     *   - Login process: POST /auth/login
     *   - Success URL:   /events
     *   - Failure URL:   /auth/login?error
     *
     * Logout:
     *   - Logout URL:     POST /auth/logout
     *   - Success URL:    /
     *   - Delete cookies: JSESSIONID, remember-me
     *
     * Remember-me:
     *   - Key: rememberMeKey (injected from application.properties)
     *   - Parameter name: "remember-me"
     *   - Token validity: 30 days
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // TODO (Alei): Replace this stub with the real security configuration.
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/events/**", "/auth/**", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .defaultSuccessUrl("/events", true)
                .failureUrl("/auth/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .rememberMe(rm -> rm
                .key(rememberMeKey)
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds(30 * 24 * 60 * 60) // 30 days
                .userDetailsService(userDetailsService())
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
