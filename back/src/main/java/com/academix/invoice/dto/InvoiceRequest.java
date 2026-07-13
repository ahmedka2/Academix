package com.academix.invoice.dto;

import com.academix.invoice.entity.InvoiceStatus;
import com.academix.invoice.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceRequest(@NotNull Long studentId, @NotNull @DecimalMin(value = "0.001") BigDecimal amount, @NotNull LocalDate issuedDate, @NotNull InvoiceStatus status, PaymentMethod paymentMethod, LocalDate paidDate) { }
