package com.evently.config;

import java.util.List;

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

import com.evently.repository.UserRepository;

/**
 * Spring Security configuration.
 *
 * Defines: - Which URLs are public vs. require authentication. - Login and
 * logout behaviour. - Remember-me token management. - UserDetailsService that
 * loads User entities from the database.
 *
 * OWNER: Alei
 *
 * TODO (Alei): 1. Complete the securityFilterChain bean below. 2. Configure
 * form login (login page, success URL, failure URL). 3. Configure logout
 * (logout URL, success URL, clear cookies). 4. Configure remember-me with the
 * APP_SECRET key and userDetailsService. 5. Wire in the UserDetailsService
 * lambda (already sketched below).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${evently.security.remember-me-key}")
    private String rememberMeKey;

    private final UserRepository userRepository;
    private final SessionTrackingAuthSuccessHandler sessionSuccessHandler;
    private final SessionTrackingLogoutHandler sessionLogoutHandler;

    public SecurityConfig(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          SessionTrackingAuthSuccessHandler sessionSuccessHandler,
                          SessionTrackingLogoutHandler sessionLogoutHandler) {
        this.userRepository        = userRepository;
        this.sessionSuccessHandler = sessionSuccessHandler;
        this.sessionLogoutHandler  = sessionLogoutHandler;
    }

    /**
     * Loads a User entity by email and wraps it in Spring Security's
     * UserDetails.
     *
     * Grants ROLE_ADMIN to admin users, ROLE_USER to everyone else. This bean
     * is used by the login form, remember-me, and session management.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            com.evently.model.User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));

            // Super admin gets ROLE_SUPER_ADMIN + ROLE_ADMIN + ROLE_USER.
            // Regular admin gets ROLE_ADMIN + ROLE_USER.
            // Everyone else gets ROLE_USER only.
            List<SimpleGrantedAuthority> authorities;
            if (user.isSuperAdmin()) {
                authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_USER")
                );
            } else if (user.isAdmin()) {
                authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_USER")
                );
            } else {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }

            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    authorities
            );
        };
    }

    /**
     * TODO (Alei): Implement the security filter chain.
     *
     * Required authorisation rules: - Public (no auth): GET /, /events/**,
     * /auth/**, /css/**, /js/** - Authenticated: /dashboard/**, /register/**,
     * /profile/** - Admin only: /admin/** (also guarded by AdminInterceptor)
     *
     * Form login: - Login page: GET /auth/login - Login process: POST
     * /auth/login - Success URL: /events - Failure URL: /auth/login?error
     *
     * Logout: - Logout URL: POST /auth/logout - Success URL: / - Delete
     * cookies: JSESSIONID, remember-me
     *
     * Remember-me: - Key: rememberMeKey (injected from application.properties)
     * - Parameter name: "remember-me" - Token validity: 30 days
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // TODO (Alei): Replace this stub with the real security configuration.
        http
                .authorizeHttpRequests(auth -> auth
                // Public pages: bare paths + wildcard sub-paths (Spring Security 6.x
                // PathPatternParser does not match /events/** against /events bare path)
                .requestMatchers("/", "/events", "/events/**",
                        "/auth/**",
                        "/css/**", "/js/**", "/images/**",
                        "/error").permitAll()
                // Super-admin-only: promote/demote other users
                .requestMatchers("/admin/users/*/promote", "/admin/users/*/demote").hasRole("SUPER_ADMIN")
                // Admin (and super admin, who also holds ROLE_ADMIN): rest of admin area
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
                )
                .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(sessionSuccessHandler)
                .failureUrl("/auth/login?error")
                .permitAll()
                )
                .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .addLogoutHandler(sessionLogoutHandler)
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
