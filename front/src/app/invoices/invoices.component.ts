import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { NotificationService } from '../core/notification.service';
import { extractErrorMessage } from '../core/http-error.util';
import { StudentResponse } from '../students/student.model';
import { StudentsService } from '../students/students.service';
import { InvoiceResponse } from './invoice.model';
import { InvoicesService } from './invoices.service';

@Component({
  selector: 'app-invoices',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './invoices.component.html',
  styleUrl: './invoices.component.css'
})
export class InvoicesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly invoicesService = inject(InvoicesService);
  private readonly studentsService = inject(StudentsService);
  private readonly notifications = inject(NotificationService);

  readonly invoices = this.invoicesService.invoices;

  loading = false;
  invoiceFilter: 'ALL' | 'PAID' | 'UNPAID' = 'ALL';
  editingInvoice: InvoiceResponse | null = null;
  invoicePendingDeletion: InvoiceResponse | null = null;
  invoiceStudentQuery = '';
  editInvoiceStudentQuery = '';
  invoicePendingPayment: InvoiceResponse | null = null;
  paymentMethod = '';

  invoiceForm = this.fb.nonNullable.group({
    studentId: [0, [Validators.min(1)]],
    amount: [0, [Validators.min(0.001)]],
    issuedDate: [new Date().toISOString().slice(0, 10), [Validators.required]],
    status: ['UNPAID'],
    paymentMethod: ['']
  });

  editInvoiceForm = this.fb.nonNullable.group({
    studentId: [0, [Validators.min(1)]],
    amount: [0, [Validators.min(0.001)]],
    issuedDate: ['', [Validators.required]],
    status: ['UNPAID'],
    paymentMethod: [''],
    paidDate: ['']
  });

  ngOnInit(): void {
    this.studentsService.load().subscribe();
    this.loadInvoices();
  }

  get filteredInvoices(): InvoiceResponse[] {
    return this.invoiceFilter === 'ALL'
      ? this.invoices()
      : this.invoices().filter((invoice) => invoice.status === this.invoiceFilter);
  }

  get paidCount(): number {
    return this.invoices().filter((invoice) => invoice.status === 'PAID').length;
  }

  get outstandingAmount(): number {
    return this.invoices()
      .filter((invoice) => invoice.status === 'UNPAID')
      .reduce((sum, invoice) => sum + invoice.amount, 0);
  }

  get invoiceStudentSuggestions(): StudentResponse[] {
    return this.studentsService.suggestionsFor(this.invoiceStudentQuery);
  }

  get editInvoiceStudentSuggestions(): StudentResponse[] {
    return this.studentsService.suggestionsFor(this.editInvoiceStudentQuery);
  }

  createInvoice(): void {
    if (this.invoiceForm.invalid) {
      this.notifications.show('Select a student and enter a valid amount.', 'error');
      return;
    }
    const value = this.invoiceForm.getRawValue();
    this.loading = true;
    this.invoicesService.create({
      ...value,
      paymentMethod: value.paymentMethod || null
    }).subscribe({
      next: (invoice) => {
        this.notifications.show(`Created invoice ${invoice.invoiceNumber}.`, 'success');
        this.invoiceForm.reset({ studentId: 0, amount: 0, issuedDate: new Date().toISOString().slice(0, 10), status: 'UNPAID', paymentMethod: '' });
        this.invoiceStudentQuery = '';
      },
      error: (error) => this.handleError(error, 'Could not create invoice.'),
      complete: () => this.loading = false
    });
  }

  loadInvoices(): void {
    this.invoicesService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load invoices.')
    });
  }

  startEditingInvoice(invoice: InvoiceResponse): void {
    this.editingInvoice = invoice;
    this.editInvoiceForm.reset({
      studentId: invoice.studentId,
      amount: invoice.amount,
      issuedDate: invoice.issuedDate,
      status: invoice.status,
      paymentMethod: invoice.paymentMethod ?? '',
      paidDate: invoice.paidDate ?? ''
    });
    this.editInvoiceStudentQuery = `${invoice.studentName} · ${invoice.studentIdentifier}`;
  }

  cancelInvoiceEditing(): void { this.editingInvoice = null; }

  updateInvoice(): void {
    if (!this.editingInvoice || this.editInvoiceForm.invalid) {
      this.notifications.show('Complete the required invoice details.', 'error');
      return;
    }
    const value = this.editInvoiceForm.getRawValue();
    this.loading = true;
    this.invoicesService.update(this.editingInvoice.id, {
      ...value,
      paymentMethod: value.paymentMethod || null,
      paidDate: value.paidDate || null
    }).subscribe({
      next: (invoice) => {
        this.editingInvoice = null;
        this.notifications.show(`Updated invoice ${invoice.invoiceNumber}.`, 'success');
      },
      error: (error) => this.handleError(error, 'Could not update invoice.'),
      complete: () => this.loading = false
    });
  }

  selectInvoiceStudent(student: StudentResponse, editing = false): void {
    const label = `${student.firstName} ${student.lastName} · ${student.studentIdentifier}`;
    if (editing) {
      this.editInvoiceForm.patchValue({ studentId: student.id });
      this.editInvoiceStudentQuery = label;
      return;
    }
    this.invoiceForm.patchValue({ studentId: student.id });
    this.invoiceStudentQuery = label;
  }

  openPaymentMethodMenu(invoice: InvoiceResponse): void {
    this.invoicePendingPayment = invoice;
    this.paymentMethod = invoice.paymentMethod ?? '';
  }

  cancelPaymentMethodMenu(): void { this.invoicePendingPayment = null; }

  markInvoicePaid(): void {
    const invoice = this.invoicePendingPayment;
    if (!invoice || !this.paymentMethod) {
      this.notifications.show('Select a payment method.', 'error');
      return;
    }
    this.loading = true;
    this.invoicesService.update(invoice.id, {
      studentId: invoice.studentId, amount: invoice.amount, issuedDate: invoice.issuedDate,
      status: 'PAID', paymentMethod: this.paymentMethod, paidDate: new Date().toISOString().slice(0, 10)
    }).subscribe({
      next: (updated) => {
        this.invoicePendingPayment = null;
        this.notifications.show(`Marked ${updated.invoiceNumber} as paid.`, 'success');
      },
      error: (error) => this.handleError(error, 'Could not mark invoice as paid.'),
      complete: () => this.loading = false
    });
  }

  requestInvoiceDelete(invoice: InvoiceResponse): void { this.invoicePendingDeletion = invoice; }
  cancelInvoiceDelete(): void { this.invoicePendingDeletion = null; }

  deleteInvoice(): void {
    const invoice = this.invoicePendingDeletion;
    if (!invoice) return;
    this.loading = true;
    this.invoicesService.delete(invoice.id).subscribe({
      next: () => {
        this.invoicePendingDeletion = null;
        this.notifications.show(`Deleted invoice ${invoice.invoiceNumber}.`, 'success');
      },
      error: (error) => this.handleError(error, 'Could not delete invoice.'),
      complete: () => this.loading = false
    });
  }

  printInvoice(invoice: InvoiceResponse): void {
    const printWindow = window.open('', '_blank', 'width=720,height=640');
    if (!printWindow) {
      this.notifications.show('Allow pop-ups to print this invoice.', 'error');
      return;
    }
    const receipt = printWindow.document.createElement('pre');
    receipt.style.cssText = 'font: 16px/1.7 system-ui,sans-serif; padding:40px; white-space:pre-wrap; color:#1c1932;';
    receipt.textContent = `ACADEMIX\nINVOICE\n\nInvoice number: ${invoice.invoiceNumber}\nStudent: ${invoice.studentName}\nStudent ID: ${invoice.studentIdentifier}\nIssue date: ${invoice.issuedDate}\nStatus: ${invoice.status}\nPayment method: ${invoice.paymentMethod ?? 'Not recorded'}\nPaid date: ${invoice.paidDate ?? 'Not paid'}\n\nAmount due: ${invoice.amount.toFixed(3)} TND`;
    printWindow.document.title = invoice.invoiceNumber;
    printWindow.document.body.append(receipt);
    printWindow.focus();
    printWindow.print();
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
