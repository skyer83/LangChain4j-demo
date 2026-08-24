package com.lulala.langchain4j.toolspecification.service;

import com.lulala.langchain4j.toolspecification.domain.entity.Booking;

import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 14:04
 */
public interface IBookingService {

    List<Booking> getAllBooking();

    Booking addBooking(String customerName, String customerSurname);

    Booking getBookingDetails(String bookingNumber);

    Booking cancelBooking(String bookingNumber);
}
