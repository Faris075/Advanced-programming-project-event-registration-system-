# Mohamed Ehab — Task Sheet
## Project: Evently Event Registration System

**Your branch:** `feature/ehab-public-events`
**Merge target:** `develop`
**Depends on:** Faris's branch (entities + repositories). Alei's SecurityConfig is helpful but not strictly required to start; public pages are unauthenticated.

---

## Your Role
You own everything the public sees before they register: the home page, the event listing page, the event detail page, the public site layout template, and all CSS/styling. Your work is the face of the application.

---

## Prerequisites
- Pull `develop` after Faris's branch is merged
- Database running, `DataSeeder` seeds 5 sample events on first run
- Start the app and verify `http://localhost:8080/events` does not crash

---

## Step-by-Step Tasks

### Step 1 — HomeController
Create `src/main/java/com/evently/controller/HomeController.java`:

```java
package com.evently.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/events";
    }
}
```

### Step 2 — PublicEventController (main feature)
Create `src/main/java/com/evently/controller/PublicEventController.java`.

This is your most important file. It handles the two main public pages.

**GET `/events` — Event listing page:**
```java
@GetMapping("/events")
public String listEvents(Model model) {
    List<Event> events = eventRepository
        .findByStatusOrderByDateTimeAsc(EventStatus.PUBLISHED);
    model.addAttribute("events", events);
    return "events/index";
}
```

**GET `/events/{id}` — Event detail page:**
```java
@GetMapping("/events/{id}")
public String showEvent(@PathVariable Long id, Model model,
                        Authentication authentication) {
    Event event = eventRepository.findById(id)
        .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));

    if (event.getStatus() != EventStatus.PUBLISHED) {
        return "redirect:/events";
    }

    long confirmedCount = registrationRepository.countConfirmedRegistrations(id);
    boolean isSoldOut = confirmedCount >= event.getCapacity();
    long waitlistCount = registrationRepository.countWaitlisted(id);

    model.addAttribute("event", event);
    model.addAttribute("confirmedCount", confirmedCount);
    model.addAttribute("spotsLeft", event.getCapacity() - confirmedCount);
    model.addAttribute("isSoldOut", isSoldOut);
    model.addAttribute("waitlistCount", waitlistCount);
    model.addAttribute("form", new RegistrationFormDto());

    // If the user is logged in, check if they are already registered
    if (authentication != null && authentication.isAuthenticated()) {
        String email = authentication.getName();
        userRepository.findByEmail(email).ifPresent(user -> {
            // You can lookup by email in attendees — left as TODO for Ahmed
            model.addAttribute("isUserRegistered", false); // placeholder
        });
    }

    return "events/show";
}
```

Required imports: `EventRepository`, `RegistrationRepository`, `UserRepository`, `Event`, `EventStatus`, `EventNotFoundException`, `RegistrationFormDto`.

Inject all four repositories via `@RequiredArgsConstructor`.

### Step 3 — Public layout template
Create `src/main/resources/templates/layout.html`. All public pages will extend this:

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title th:text="${pageTitle != null ? pageTitle + ' — Evently' : 'Evently'}">Evently</title>
    <link rel="stylesheet" th:href="@{/css/app.css}" />
</head>
<body>
<header>
    <nav class="navbar">
        <a class="brand" th:href="@{/}">Evently</a>
        <ul class="nav-links">
            <li><a th:href="@{/events}">Events</a></li>
            <li sec:authorize="isAuthenticated()">
                <a th:href="@{/dashboard}">My Registrations</a>
            </li>
            <li sec:authorize="hasRole('ADMIN')">
                <a th:href="@{/admin}">Admin</a>
            </li>
            <li sec:authorize="isAuthenticated()">
                <a th:href="@{/profile}">Profile</a>
            </li>
            <li sec:authorize="isAnonymous()">
                <a th:href="@{/auth/login}">Login</a>
            </li>
            <li sec:authorize="isAuthenticated()">
                <form th:action="@{/auth/logout}" method="post">
                    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
                    <button type="submit" class="btn-link">Logout</button>
                </form>
            </li>
        </ul>
    </nav>
</header>
<main class="container" th:fragment="content">
    <!-- child pages replace this -->
</main>
<footer>
    <p>© 2024 Evently</p>
</footer>
</body>
</html>
```

### Step 4 — Event listing template
Create `src/main/resources/templates/events/index.html`:

Show all published events as cards. Each card should display:
- Event title
- Date + time (format with `#temporals.format(event.dateTime, 'MMM dd, yyyy HH:mm')`)
- Location
- Price (or "Free" if `event.price` is null or zero)
- Capacity (e.g. "48 / 100 spots filled" — note: the count is not available here without a sub-query, so you can show just capacity)
- A "View Details" link to `/events/{id}`

Example card snippet:
```html
<div th:each="event : ${events}" class="event-card">
    <h3 th:text="${event.title}">Event Title</h3>
    <p th:text="${#temporals.format(event.dateTime, 'MMM dd, yyyy  HH:mm')}">Date</p>
    <p th:text="${event.location}">Location</p>
    <p th:text="${event.price != null ? '$' + event.price : 'Free'}">Price</p>
    <a th:href="@{/events/{id}(id=${event.id})}" class="btn">View Details</a>
</div>
```

Thymeleaf temporal dialects for date formatting require `thymeleaf-extras-java8time` — it is already included in `pom.xml` via Spring Boot's Thymeleaf starter.

### Step 5 — Event detail template
Create `src/main/resources/templates/events/show.html`:

Display:
- Full event title, description, date/time, location, capacity
- "X spots left" or "Sold Out" banner
- Price or "Free"
- Waitlist size message if sold out (e.g. "12 people on waitlist")
- **Registration form** (this is the form Ahmed will handle the POST for — your job is to render it)

The registration form:
```html
<form th:action="@{/register/{id}(id=${event.id})}" method="post">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
    <input type="text"  th:field="*{form.name}"  placeholder="Full Name"  required />
    <input type="email" th:field="*{form.email}" placeholder="Email"       required />
    <input type="tel"   th:field="*{form.phone}" placeholder="Phone (optional)" />
    <button type="submit" th:text="${isSoldOut ? 'Join Waitlist' : 'Register Now'}">
        Register
    </button>
</form>
```

For the form binding to work, `model.addAttribute("form", new RegistrationFormDto())` must be in the controller (already done in Step 2).

### Step 6 — CSS styling
Create `src/main/resources/static/css/app.css`.

Minimum required styles:
- CSS custom properties for light theme: `--color-primary`, `--color-bg`, `--color-text`, `--color-card-bg`, `--color-border`
- Optional dark theme with `@media (prefers-color-scheme: dark)`
- `.navbar` horizontal flex layout
- `.container` max-width 1100px, centered
- `.event-card` card with border, border-radius, padding, box-shadow
- `.btn` primary button styles
- Basic form input styles
- `.badge-sold-out` red badge

Also create `src/main/resources/static/css/admin.css` as a minimal stylesheet for admin (Morsy needs a stylesheet to reference).

---

## Files You Own

| File | Package / Path |
|------|---------------|
| `HomeController.java` | `com.evently.controller` |
| `PublicEventController.java` | `com.evently.controller` |
| `layout.html` | `templates/` |
| `events/index.html` | `templates/events/` |
| `events/show.html` | `templates/events/` |
| `static/css/app.css` | `src/main/resources/static/css/` |
| `static/css/admin.css` | `src/main/resources/static/css/` |

---

## Interface Contract with Ahmed
Ahmed's `RegistrationController` handles `POST /register/{id}`. Your `events/show.html` form must POST to that exact URL. The form model object must be named `"form"` and be of type `RegistrationFormDto`. Do not change the field names.

**`RegistrationFormDto` fields:**
- `name` (String)
- `email` (String)
- `phone` (String, optional)

These must match exactly between your template and Ahmed's controller binding.

---

## Common Mistakes to Avoid
- `th:object="${form}"` must match the model attribute name `"form"`. Do not rename it.
- Do NOT add any `@RestController` — all controllers must be `@Controller` returning view names.
- All public routes (`/`, `/events`, `/events/**`) are permitted without authentication in `SecurityConfig`. Do not add any `@PreAuthorize("isAuthenticated()")` to `PublicEventController`.
- For Thymeleaf date formatting use `#temporals` (not `#dates` — that is for legacy `java.util.Date`).
