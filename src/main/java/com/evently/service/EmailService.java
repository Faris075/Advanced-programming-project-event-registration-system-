package com.evently.service;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.evently.model.Event;
import com.evently.model.Registration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Send confirmation email for confirmed registrations.
     */
    @Async
    public void sendConfirmationEmail(Registration registration) {

        try {

            Event event = registration.getEvent();

            SimpleMailMessage msg = new SimpleMailMessage();

            msg.setTo(registration.getAttendee().getEmail());

            msg.setSubject(
                    "Registration Confirmed — " + event.getTitle()
            );

            msg.setText(String.format(
                    """
                    Hello %s,

                    Your registration for "%s" has been confirmed.

                    Event Details:
                    -------------------------
                    Event: %s
                    Date & Time: %s

                    Thank you for using Evently!
                    """,
                    registration.getAttendee().getName(),
                    event.getTitle(),
                    event.getTitle(),
                    event.getDateTime()
            ));

            mailSender.send(msg);

            log.info(
                    "Confirmation email sent to {}",
                    registration.getAttendee().getEmail()
            );

        } catch (MailException e) {

            log.error(
                    "Failed to send confirmation email for registration {}: {}",
                    registration.getId(),
                    e.getMessage()
            );
        }
    }

    /**
     * Send waitlist email.
     */
    @Async
    public void sendWaitlistEmail(Registration registration) {

        try {

            Event event = registration.getEvent();

            SimpleMailMessage msg = new SimpleMailMessage();

            msg.setTo(registration.getAttendee().getEmail());

            msg.setSubject(
                    "You're on the Waitlist — " + event.getTitle()
            );

            msg.setText(String.format(
                    """
                    Hello %s,

                    The event "%s" is currently full.

                    You have been added to the waitlist.

                    Your waitlist position is: #%d

                    We will notify you automatically if a spot becomes available.

                    Thank you for using Evently!
                    """,
                    registration.getAttendee().getName(),
                    event.getTitle(),
                    registration.getWaitlistPosition()
            ));

            mailSender.send(msg);

            log.info(
                    "Waitlist email sent to {}",
                    registration.getAttendee().getEmail()
            );

        } catch (MailException e) {

            log.error(
                    "Failed to send waitlist email for registration {}: {}",
                    registration.getId(),
                    e.getMessage()
            );
        }
    }

    /**
     * Send promotion email when moved from waitlist to confirmed.
     */
    @Async
    public void sendWaitlistPromotionEmail(Registration registration) {

        try {

            Event event = registration.getEvent();

            SimpleMailMessage msg = new SimpleMailMessage();

            msg.setTo(registration.getAttendee().getEmail());

            msg.setSubject(
                    "Great News — You Got a Spot! — " + event.getTitle()
            );

            msg.setText(String.format(
                    """
                    Hello %s,

                    Great news!

                    A spot has become available for "%s".

                    Your registration is now CONFIRMED.

                    Event Date & Time:
                    %s

                    We look forward to seeing you!

                    Thank you for using Evently!
                    """,
                    registration.getAttendee().getName(),
                    event.getTitle(),
                    event.getDateTime()
            ));

            mailSender.send(msg);

            log.info(
                    "Promotion email sent to {}",
                    registration.getAttendee().getEmail()
            );

        } catch (MailException e) {

            log.error(
                    "Failed to send promotion email for registration {}: {}",
                    registration.getId(),
                    e.getMessage()
            );
        }
    }
}