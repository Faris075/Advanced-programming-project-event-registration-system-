package com.evently.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.evently.config.PasswordEncoderConfig;
import com.evently.config.SecurityConfig;
import com.evently.model.Event;
import com.evently.model.EventStatus;
import com.evently.repository.EventRepository;
import com.evently.repository.RegistrationRepository;
import com.evently.repository.UserRepository;

@WebMvcTest(PublicEventController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventRepository eventRepository;

    @MockBean
    private RegistrationRepository registrationRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getEvents_returns200WithEventList() throws Exception {
        Event event = Event.builder()
                .id(1L)
                .title("Test Event")
                .dateTime(LocalDateTime.now().plusDays(7))
                .status(EventStatus.PUBLISHED)
                .build();

        Page<Event> events = new PageImpl<>(List.of(event),
                PageRequest.of(0, 10), 1);

        when(eventRepository.findByStatusOrderByDateTimeAsc(EventStatus.PUBLISHED,
                PageRequest.of(0, 10))).thenReturn(events);

        mockMvc.perform(get("/events").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("events"));
    }

    @Test
    void getEventDetail_returns200ForPublishedEvent() throws Exception {
        Event event = Event.builder()
                .id(1L)
                .title("Published Event")
                .status(EventStatus.PUBLISHED)
                .dateTime(LocalDateTime.now().plusDays(3))
                .capacity(50)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.countConfirmedRegistrations(1L)).thenReturn(0L);
        when(registrationRepository.countWaitlisted(1L)).thenReturn(0L);

        mockMvc.perform(get("/events/1").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Test
    void getEventDetail_redirectsForUnpublishedEvent() throws Exception {
        Event event = Event.builder()
                .id(1L)
                .title("Draft Event")
                .status(EventStatus.DRAFT)
                .dateTime(LocalDateTime.now().plusDays(3))
                .capacity(20)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        mockMvc.perform(get("/events/1").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events"));
    }
}
