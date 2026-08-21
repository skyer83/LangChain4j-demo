package com.lulala.langchain4j.toolspecification.service;

import com.lulala.langchain4j.toolspecification.domain.entity.Booking;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 14:04
 */
public interface IBookingService {

    Booking addBooking(String customerName, String customerSurname);

    Booking getBookingDetails(String bookingNumber);

    void cancelBooking(String bookingNumber);
}
