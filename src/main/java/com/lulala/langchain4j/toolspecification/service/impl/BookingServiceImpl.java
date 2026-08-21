package com.lulala.langchain4j.toolspecification.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.lulala.langchain4j.toolspecification.domain.entity.Booking;
import com.lulala.langchain4j.toolspecification.service.IBookingService;
import com.lulala.langchain4j.utils.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 14:26
 */
@Slf4j
@Service
public class BookingServiceImpl implements IBookingService {

    private static List<Booking> bookings = new ArrayList<>();

    static {
        bookings.addAll(List.of(
                new Booking(IdWorker.getSnowflakeNextId(), "1", "John", "Doe"),
                new Booking(IdWorker.getSnowflakeNextId(), "2", "Jane", "Smith"),
                new Booking(IdWorker.getSnowflakeNextId(), "3", "Mike", "Johnson")
        ));
    }

    @Override
    public Booking addBooking(String customerName, String customerSurname) {
        String bookingNumber = "1";
        if (CollectionUtil.isNotEmpty(bookings)) {
            bookingNumber = String.valueOf(Integer.parseInt(bookings.getLast().getBookingNumber()) + 1);
        }
        Booking booking = new Booking(IdWorker.getSnowflakeNextId(), bookingNumber, customerName, customerSurname);
        bookings.add(booking);
        log.info("新增预约号：{}。\n当前所有预约：{}", bookingNumber, bookings);
        return booking;
    }

    @Override
    public Booking getBookingDetails(String bookingNumber) {
        return bookings.stream().filter(item -> item.getBookingNumber().equals(bookingNumber)).findFirst().orElse(null);
    }

    @Override
    public void cancelBooking(String bookingNumber) {
        boolean cancelBooking = bookings.removeIf(item -> item.getBookingNumber().equals(bookingNumber));
        if (cancelBooking) {
            log.info("Canceling booking with booking number {}", bookingNumber);
        }
    }
}
