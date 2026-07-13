package com.academix.invoice.service;

import com.academix.invoice.dto.InvoiceRequest;
import com.academix.invoice.dto.InvoiceResponse;
import com.academix.invoice.entity.Invoice;
import com.academix.invoice.entity.InvoiceStatus;
import com.academix.invoice.repository.InvoiceRepository;
import com.academix.student.entity.Student;
import com.academix.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

@Service @Transactional
public class InvoiceService {
	private static final SecureRandom RANDOM = new SecureRandom();
	private final InvoiceRepository invoiceRepository; private final StudentRepository studentRepository;
	public InvoiceService(InvoiceRepository invoiceRepository, StudentRepository studentRepository) { this.invoiceRepository = invoiceRepository; this.studentRepository = studentRepository; }
	@Transactional(readOnly = true) public List<InvoiceResponse> getAll() { return invoiceRepository.findAll().stream().map(this::toResponse).toList(); }
	public InvoiceResponse create(InvoiceRequest request) { Invoice invoice = new Invoice(student(request.studentId()), number(), request.amount(), request.issuedDate(), request.status(), request.paymentMethod(), paidDate(request)); return toResponse(invoiceRepository.save(invoice)); }
	public InvoiceResponse update(Long id, InvoiceRequest request) { Invoice invoice = invoice(id); invoice.setStudent(student(request.studentId())); invoice.setAmount(request.amount()); invoice.setIssuedDate(request.issuedDate()); invoice.setStatus(request.status()); invoice.setPaymentMethod(request.paymentMethod()); invoice.setPaidDate(paidDate(request)); return toResponse(invoice); }
	public void delete(Long id) { invoiceRepository.delete(invoice(id)); }
	private Invoice invoice(Long id) { return invoiceRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found")); }
	private Student student(Long id) { return studentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found")); }
	private LocalDate paidDate(InvoiceRequest request) { return request.status() == InvoiceStatus.PAID ? (request.paidDate() == null ? LocalDate.now() : request.paidDate()) : null; }
	private String number() { for (int i=0; i<100; i++) { String candidate = "INV-" + LocalDate.now().getYear() + "-" + String.format("%06d", RANDOM.nextInt(1_000_000)); if (!invoiceRepository.existsByInvoiceNumber(candidate)) return candidate; } throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not generate invoice number"); }
	private InvoiceResponse toResponse(Invoice invoice) { Student s=invoice.getStudent(); return new InvoiceResponse(invoice.getId(),s.getId(),s.getFirstName()+" "+s.getLastName(),s.getStudentIdentifier(),invoice.getInvoiceNumber(),invoice.getAmount(),invoice.getIssuedDate(),invoice.getStatus(),invoice.getPaymentMethod(),invoice.getPaidDate()); }
}
