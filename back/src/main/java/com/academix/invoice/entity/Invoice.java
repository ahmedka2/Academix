package com.academix.invoice.entity;

import com.academix.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "invoices", uniqueConstraints = @UniqueConstraint(name = "uk_invoices_number", columnNames = "invoiceNumber"))
public class Invoice {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false) private Student student;
	@Column(nullable = false, length = 40) private String invoiceNumber;
	@Column(nullable = false, precision = 12, scale = 3) private BigDecimal amount;
	@Column(nullable = false) private LocalDate issuedDate;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private InvoiceStatus status;
	@Enumerated(EnumType.STRING) @Column(length = 30) private PaymentMethod paymentMethod;
	private LocalDate paidDate;
	@Column(nullable = false, updatable = false) private Instant createdAt;

	public Invoice() { }
	public Invoice(Student student, String invoiceNumber, BigDecimal amount, LocalDate issuedDate, InvoiceStatus status, PaymentMethod paymentMethod, LocalDate paidDate) { this.student = student; this.invoiceNumber = invoiceNumber; this.amount = amount; this.issuedDate = issuedDate; this.status = status; this.paymentMethod = paymentMethod; this.paidDate = paidDate; }
	@PrePersist void onCreate() { createdAt = Instant.now(); }
	public Long getId() { return id; } public Student getStudent() { return student; } public String getInvoiceNumber() { return invoiceNumber; } public BigDecimal getAmount() { return amount; } public LocalDate getIssuedDate() { return issuedDate; } public InvoiceStatus getStatus() { return status; } public PaymentMethod getPaymentMethod() { return paymentMethod; } public LocalDate getPaidDate() { return paidDate; }
	public void setStudent(Student student) { this.student = student; } public void setAmount(BigDecimal amount) { this.amount = amount; } public void setIssuedDate(LocalDate issuedDate) { this.issuedDate = issuedDate; } public void setStatus(InvoiceStatus status) { this.status = status; } public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; } public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }
}
