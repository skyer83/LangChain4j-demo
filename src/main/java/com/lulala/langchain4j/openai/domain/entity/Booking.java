package com.lulala.langchain4j.openai.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Booking JPA 实体 - 预订信息
 *
 * @author shenjh
 * @version 1.0
 * @since 2026/6/8 14:09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    Long id;
    String bookingNumber;
    String customerName;
    String customerSurname;
}
