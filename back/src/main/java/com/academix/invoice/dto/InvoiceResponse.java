package com.academix.invoice.dto;

import com.academix.invoice.entity.InvoiceStatus;
import com.academix.invoice.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceResponse(Long id, Long studentId, String studentName, String studentIdentifier, String invoiceNumber, BigDecimal amount, LocalDate issuedDate, InvoiceStatus status, PaymentMethod paymentMethod, LocalDate paidDate) { }
