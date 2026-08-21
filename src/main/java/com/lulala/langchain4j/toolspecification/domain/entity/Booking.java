package com.lulala.langchain4j.toolspecification.domain.entity;

import dev.langchain4j.model.output.structured.Description;
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
@Description("预约信息")
public class Booking {

    @Description("预约ID")
    Long id;
    @Description("预约编号")
    String bookingNumber;
    @Description("客户姓名")
    String customerName;
    @Description("客户姓氏")
    String customerSurname;
}
