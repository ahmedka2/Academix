package com.academix.invoice.controller;

import com.academix.invoice.dto.InvoiceRequest;
import com.academix.invoice.dto.InvoiceResponse;
import com.academix.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/invoices")
public class InvoiceController {
	private final InvoiceService service; public InvoiceController(InvoiceService service) { this.service=service; }
	@GetMapping public List<InvoiceResponse> getAll() { return service.getAll(); }
	@GetMapping("/me") public List<InvoiceResponse> getMine() { return service.getMine(); }
	@PostMapping public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
	@PutMapping("/{id}") public InvoiceResponse update(@PathVariable Long id, @Valid @RequestBody InvoiceRequest request) { return service.update(id, request); }
	@DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
