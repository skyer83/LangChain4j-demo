package com.lulala.langchain4j.openai.service;

import com.lulala.langchain4j.openai.domain.entity.Booking;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 14:04
 */
public interface IBookingService {

    Booking getBookingDetails(String bookingNumber, String customerName, String customerSurname);

    void cancelBooking(String bookingNumber, String customerName, String customerSurname);
}
