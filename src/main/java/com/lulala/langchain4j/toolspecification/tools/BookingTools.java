package com.lulala.langchain4j.toolspecification.tools;

import com.lulala.langchain4j.toolspecification.domain.entity.Booking;
import com.lulala.langchain4j.toolspecification.service.IBookingService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 14:03
 */
@Slf4j
@Component
public class BookingTools {

    private final IBookingService bookingService;

    public BookingTools(IBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Tool("获取所有预约详情")
    public List<Booking> getAllBooking() {
        return bookingService.getAllBooking();
    }

    @Tool("新增预约")
    public Booking addBooking(@P("客户姓名") String customerName, @P("客户姓氏") String customerSurname) {
        return bookingService.addBooking(customerName, customerSurname);
    }

    @Tool("获取预约详情")
    public Booking getBookingDetails(@P("预约编号") String bookingNumber) {
        return bookingService.getBookingDetails(bookingNumber);
    }

    @Tool("取消预约")
    public Booking cancelBooking(@P("预约编号") String bookingNumber) {
        return bookingService.cancelBooking(bookingNumber);
    }

}
