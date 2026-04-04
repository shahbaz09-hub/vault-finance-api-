package com.financeapp.dto.response;

import com.financeapp.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendResponse {
    private Integer year;
    private Integer month;
    private TransactionType type;
    private BigDecimal totalAmount;
}
