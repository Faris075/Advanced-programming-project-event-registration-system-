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

    
