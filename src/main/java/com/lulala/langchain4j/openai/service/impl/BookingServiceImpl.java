package com.lulala.langchain4j.openai.service.impl;

import com.lulala.langchain4j.openai.domain.entity.Booking;
import com.lulala.langchain4j.openai.service.IBookingService;
import com.lulala.langchain4j.utils.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 14:26
 */
@Slf4j
@Service
public class BookingServiceImpl implements IBookingService {

    @Override
    public Booking getBookingDetails(String bookingNumber, String customerName, String customerSurname) {
        return new Booking(IdWorker.nextDidiId(), bookingNumber, customerName, customerSurname);
    }

    @Override
    public void cancelBooking(String bookingNumber, String customerName, String customerSurname) {
        log.info("Canceling booking for {} {} with booking number {}", customerName, customerSurname, bookingNumber);
    }
}
