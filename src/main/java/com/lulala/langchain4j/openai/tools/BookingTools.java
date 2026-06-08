package com.lulala.langchain4j.openai.tools;

import com.lulala.langchain4j.openai.domain.entity.Booking;
import com.lulala.langchain4j.openai.service.IBookingService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 14:03
 */
@Component
public class BookingTools {

    private final IBookingService bookingService;

    public BookingTools(IBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Tool
    public Booking getBookingDetails(String bookingNumber, String customerName, String customerSurname) {
        return bookingService.getBookingDetails(bookingNumber, customerName, customerSurname);
    }

    @Tool
    public void cancelBooking(String bookingNumber, String customerName, String customerSurname) {
        bookingService.cancelBooking(bookingNumber, customerName, customerSurname);
    }

}
