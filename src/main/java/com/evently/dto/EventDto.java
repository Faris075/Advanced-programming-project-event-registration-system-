package com.evently.dto;

import com.evently.model.EventStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Form-backing bean for the admin create/edit event form.
 *
 * OWNER: Mohamed Morsy
 */
@Getter
@Setter
public class EventDto {

    @NotBlank(message = "Title is required.")
    private String title;

    @NotBlank(message = "Description is required.")
    private String description;

    @NotNull(message = "Date and time is required.")
    @Future(message = "Event date must be in the future.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateTime;

    @NotBlank(message = "Location is required.")
    private String location;

    @NotNull(message = "Capacity is required.")
    @Min(value = 1, message = "Capacity must be at least 1.")
    private Integer capacity;

    /**
     * Optional — null means free event.
     */
    private BigDecimal price;

    private EventStatus status = EventStatus.DRAFT;
}
