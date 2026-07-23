package com.academix.statistics.dto;

import java.math.BigDecimal;

public record PaymentStatsResponse(
		long totalInvoices,
		long paidCount,
		long unpaidCount,
		BigDecimal totalAmount,
		BigDecimal paidAmount,
		BigDecimal outstandingAmount) {
}
