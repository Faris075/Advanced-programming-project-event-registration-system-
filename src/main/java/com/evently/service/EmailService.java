package com.evently.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.evently.model.Event;
import com.evently.model.Registration;

import lombok.extern.slf4j.Slf4j;

/**
 * Sends transactional emails via the Mailtrap HTTP API.
 *
 * <p>Sandbox (Email Testing): POST https://sandbox.api.mailtrap.io/api/send/{inboxId}
 * <br>Sending (Production):   POST https://send.api.mailtrap.io/api/send
 *
 * <p>Set MAILTRAP_API_TOKEN and MAILTRAP_INBOX_ID in your .env file.
 */
@Service
@Slf4j
public class EmailService {

    private final RestClient restClient;
    private final String inboxId;
    private final String fromEmail;

    public EmailService(
            @Value("${mailtrap.api.token:}") String apiToken,
            @Value("${mailtrap.inbox.id:}") String inboxId,
            @Value("${mailtrap.from.email:noreply@evently.dev}") String fromEmail) {

        this.inboxId = inboxId;
        this.fromEmail = fromEmail;

        String baseUrl = (inboxId != null && !inboxId.isBlank())
                ? "https://sandbox.api.mailtrap.io"
                : "https://send.api.mailtrap.io";

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiToken)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Async
    public void sendConfirmationEmail(Registration registration) {
        try {
            Event event = registration.getEvent();
            String subject = "Registration Confirmed — " + event.getTitle();
            String body = String.format(
                    "Hello %s,%n%n" +
                    "Your registration for \"%s\" has been confirmed!%n%n" +
                    "Event Details:%n" +
                    "  Date & Time : %s%n" +
                    "  Location    : %s%n%n" +
                    "We look forward to seeing you there!%n%n" +
                    "Best regards,%n" +
                    "The Evently Team",
                    registration.getAttendee().getName(),
                    event.getTitle(),
                    event.getDateTime(),
                    event.getLocation());

            send(registration.getAttendee().getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send confirmation email for registration {}: {}",
                    registration.getId(), e.getMessage());
        }
    }

    @Async
    public void sendWaitlistEmail(Registration registration) {
        try {
            Event event = registration.getEvent();
            String subject = "You're on the Waitlist — " + event.getTitle();
            String body = String.format(
                    "Hello %s,%n%n" +
                    "The event \"%s\" is currently full, but you have been added to the waitlist.%n" +
                    "Your position: #%d%n%n" +
                    "We will notify you automatically if a spot opens up.%n%n" +
                    "Best regards,%n" +
                    "The Evently Team",
                    registration.getAttendee().getName(),
                    event.getTitle(),
                    registration.getWaitlistPosition());

            send(registration.getAttendee().getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send waitlist email for registration {}: {}",
                    registration.getId(), e.getMessage());
        }
    }

    @Async
    public void sendWaitlistPromotionEmail(Registration registration) {
        try {
            Event event = registration.getEvent();
            String subject = "Great news — You got a spot! — " + event.getTitle();
            String body = String.format(
                    "Hello %s,%n%n" +
                    "Good news! A spot has opened up for \"%s\" and you have been confirmed.%n%n" +
                    "Event Details:%n" +
                    "  Date & Time : %s%n" +
                    "  Location    : %s%n%n" +
                    "We look forward to seeing you there!%n%n" +
                    "Best regards,%n" +
                    "The Evently Team",
                    registration.getAttendee().getName(),
                    event.getTitle(),
                    event.getDateTime(),
                    event.getLocation());

            send(registration.getAttendee().getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send promotion email for registration {}: {}",
                    registration.getId(), e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Private helper
    // -----------------------------------------------------------------------

    private void send(String toEmail, String subject, String text) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping email '{}' — recipient address is blank", subject);
            return;
        }

        Map<String, Object> payload = Map.of(
                "from", Map.of("email", fromEmail, "name", "Evently"),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "text", text);

        String path = (inboxId != null && !inboxId.isBlank())
                ? "/api/send/" + inboxId
                : "/api/send";

        try {
            restClient.post()
                    .uri(path)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Email sent to {} — {}", toEmail, subject);
        } catch (RestClientException e) {
            log.error("Mailtrap API error sending to {}: {}", toEmail, e.getMessage());
        }
    }
}




@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    
    @Async 

    public void sendConfirmationEmail(Registration registration) {


        try {

              Event event = registration.getEvent();
            SimpleMailMessage msg = new SimpleMailMessage();
          msg.setTo(registration.getAttendee().getEmail());
            msg.setSubject("Registration Confirmed" + event.getTitle());
            msg.setText(String.format(registration.getAttendee().getName(), event.getTitle(), event.getDateTime() ) );



            mailSender.send(msg);

        }


        catch (MailException e) {
         log.error("Failed to send confirmation email for registration{}: {}",
                      registration.getId(), e.getMessage());
        }
    }

@Async


public void sendWaitlistEmail(Registration registration) {
        try {
              Event event = registration.getEvent();
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(registration.getAttendee().getEmail());
            msg.setSubject("You're on the Waitlist — " + event.getTitle());
            msg.setText(String.format(
                registration.getAttendee().getName(),event.getTitle(),
                registration.getWaitlistPosition()
            ));
            mailSender.send(msg);
        } catch (MailException e) {


            log.error("Failed to send waitlist email for registration {}: {}",
                      registration.getId(), e.getMessage());

        }
    }




    @Async
    public void sendWaitlistPromotionEmail(Registration registration) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(registration.getAttendee().getEmail());
            msg.setSubject("Great news — You got a spot! — " + registration.getEvent().getTitle());
            msg.setText(String.format(registration.getAttendee().getName(),registration.getEvent().getTitle()
            ));
            mailSender.send(msg);
        } catch (MailException e) {
            log.error("Failed to send promotion email for registration {}: {}",
                      registration.getId(), e.getMessage());
        }
    }




    }

    
