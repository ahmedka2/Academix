package com.academix.invoice.repository;

import com.academix.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
	boolean existsByInvoiceNumber(String invoiceNumber);
}
