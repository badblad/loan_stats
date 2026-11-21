package com.example.loan_stats;

import java.math.BigDecimal;

public record LoanStatsResponse(
        String attributeName,
        BigDecimal minLoanAmount,
        BigDecimal maxLoanAmount,
        BigDecimal averageLoanAmount,
        BigDecimal medianLoanAmount) {
}
