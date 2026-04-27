# Alei — Task Sheet
## Project: Evently Event Registration System

**Your branch:** `feature/alei-auth`
**Merge target:** `develop`
**Depends on:** Faris's branch must be merged first so you have the entities and `PasswordEncoderConfig` bean.

---

## Your Role
You own the entire authentication layer: user registration, login, logout, forgot-password via security question, profile management, and the admin user management screen. You also write `SecurityConfig`, which is the heart of how Spring Security protects the whole application.

---

## Prerequisites
- Pull `develop` after Faris's branch is merged
- Run the database schema (`schema.sql`) if you haven't already
- Set `DB_USERNAME` / `DB_PASSWORD` in your local environment (or `application-local.properties`)
- The `BCryptPasswordEncoder` bean is already defined in `PasswordEncoderConfig.java` (Faris created it) — inject it, don't create it yourself

---

## Step-by-Step Tasks

### Step 1 — Spring Security configuration
Create `src/main/java/com/evently/config/SecurityConfig.java`:

```java
package com.evently.config;

import com.evently.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            com.evently.model.User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            String role = user.isAdmin() ? "ROLE_ADMIN" : "ROLE_USER";
            return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(role))
            );
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/events", "/events/**",
                                 "/auth/**", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/auth/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout")
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .rememberMe(rem -> rem
                .key("evently-dev-secret-change-me")
                .tokenValiditySeconds(30 * 24 * 60 * 60)   // 30 days
                .rememberMeParameter("remember_me")
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/admin/api/**"));
        return http.build();
    }
}
```

### Step 2 — AuthController (register & login)
Create `src/main/java/com/evently/controller/auth/AuthController.java`:

- **GET `/auth/login`** → return `"auth/login"` (Thymeleaf template). If user is already authenticated, redirect to `/dashboard`.
- **GET `/auth/register`** → return `"auth/register"`, add `new UserRegistrationDto()` to model as `"form"`
- **POST `/auth/register`** → validate `@Valid` `UserRegistrationDto`, check `password.equals(passwordConfirm)`, check `userRepository.existsByEmail(email)`, hash password with `BCryptPasswordEncoder`, save a new `User` with `isAdmin = false`, redirect to `/auth/login?registered`

Key validation to add manually in the POST handler:
```java
if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
    result.rejectValue("passwordConfirm", "password.mismatch", "Passwords do not match");
    return "auth/register";
}
if (userRepository.existsByEmail(dto.getEmail())) {
    result.rejectValue("email", "email.taken", "An account with this email already exists");
    return "auth/register";
}
```

### Step 3 — SecurityQuestionController (forgot password)
Create `src/main/java/com/evently/controller/auth/SecurityQuestionController.java`.

This is a multi-step flow. Use `HttpSession` to carry state across steps:

| Step | URL | What happens |
|------|-----|-------------|
| 1 | GET `/auth/forgot-password` | Show "enter your email" form |
| 2 | POST `/auth/forgot-password` | Look up user by email, if found store user ID in session, redirect to step 3; else show error |
| 3 | GET `/auth/security-question` | Load user from session, display their `securityQuestion` |
| 4 | POST `/auth/security-question` | BCrypt-check answer. If correct, mark session as `securityVerified=true`, redirect to step 5; else show error |
| 5 | GET `/auth/reset-password` | Show new-password form (only if `securityVerified=true` in session, else redirect to step 1) |
| 6 | POST `/auth/reset-password` | Validate & confirm password, BCrypt-hash new password, update user, clear session flags, redirect to `/auth/login?reset` |

**Important security rule:** Never expose the security question if the email doesn't exist. Show the same "email not found" message as any other validation error to prevent email enumeration.

### Step 4 — ProfileController
Create `src/main/java/com/evently/controller/auth/ProfileController.java`:

- Annotate the class with `@PreAuthorize("isAuthenticated()")` or check `SecurityContextHolder` in each method
- **GET `/profile`** → load current user by email from `SecurityContextHolder`, return `"auth/profile"` with model
- **POST `/profile/update-name`** → update `user.name`, save, redirect back with `?updated`
- **POST `/profile/change-password`** → verify old password with BCrypt, check new/confirm match, hash and save new password, invalidate session, redirect to `/auth/login?passwordChanged`
- **POST `/profile/set-security-question`** → save `securityQuestion` and BCrypt-hashed `securityAnswer`
- **POST `/profile/currency`** → update `currencyPreference` (accept one of: `USD`, `EUR`, `EGP`, `GBP`), redirect back

Helper — get the logged-in `User` entity:
```java
private com.evently.model.User getCurrentUser() {
    String email = ((org.springframework.security.core.userdetails.UserDetails)
        SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
}
```

### Step 5 — AdminUserController
Create `src/main/java/com/evently/controller/admin/AdminUserController.java`:
- **GET `/admin/users`** → paginated list of all users (use `Pageable` from Spring Data), return `"admin/users/index"` template
- **GET `/admin/users/{id}`** → show user details + their registrations, return `"admin/users/show"` template
- **POST `/admin/users/{id}/toggle-admin`** → flip `isAdmin` boolean and save. Prevent toggling yourself (compare with logged-in user's ID). Redirect back.
- **POST `/admin/users/{id}/reset-password`** → set a randomly generated 10-character password (use `UUID.randomUUID().toString().substring(0,10)`), BCrypt it, save, add new plaintext to flash attributes so admin can see it once.

### Step 6 — Create Thymeleaf templates
Create all templates in `src/main/resources/templates/auth/` and `src/main/resources/templates/admin/users/`:

| Template | Purpose |
|----------|---------|
| `auth/login.html` | Login form with email + password + remember-me checkbox |
| `auth/register.html` | Registration form with all 3 fields + validation errors |
| `auth/forgot-password.html` | Email input form (step 1) |
| `auth/security-question.html` | Display question, input for answer (step 3) |
| `auth/reset-password.html` | New password + confirm form (step 5) |
| `auth/profile.html` | Profile page with 4 forms (name, password, security question, currency) |
| `admin/users/index.html` | Paginated user table with search |
| `admin/users/show.html` | User detail + registration history |

---

## DTOs You Need (create if not already present)

**`UserRegistrationDto.java`** in `com.evently.dto`:
```java
@Getter @Setter
public class UserRegistrationDto {
    @NotBlank private String name;
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") private String password;
    @NotBlank private String passwordConfirm;
}
```

---

## Files You Own

| File | Package / Path |
|------|---------------|
| `SecurityConfig.java` | `com.evently.config` |
| `AuthController.java` | `com.evently.controller.auth` |
| `SecurityQuestionController.java` | `com.evently.controller.auth` |
| `ProfileController.java` | `com.evently.controller.auth` |
| `AdminUserController.java` | `com.evently.controller.admin` |
| `UserRegistrationDto.java` | `com.evently.dto` |
| `auth/login.html` | `templates/auth/` |
| `auth/register.html` | `templates/auth/` |
| `auth/forgot-password.html` | `templates/auth/` |
| `auth/security-question.html` | `templates/auth/` |
| `auth/reset-password.html` | `templates/auth/` |
| `auth/profile.html` | `templates/auth/` |
| `admin/users/index.html` | `templates/admin/users/` |
| `admin/users/show.html` | `templates/admin/users/` |

---

## Common Mistakes to Avoid
- **Do NOT** create a second `PasswordEncoderConfig` — Faris already has one. Just inject `BCryptPasswordEncoder`.
- **Do NOT** use `@EnableWebSecurity` on any other class.
- The `UserDetailsService` lambda must load the user from `UserRepository`, check `isAdmin()`, and grant `ROLE_ADMIN` or `ROLE_USER` accordingly.
- Always BCrypt-hash passwords before saving to the database. Never store plaintext.
- In Thymeleaf login form: `<form th:action="@{/auth/login}" method="post">` — Spring Security handles the POST automatically.
