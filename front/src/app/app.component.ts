import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

interface AuthResponse {
  token: string;
  tokenType: string;
  user: {
    id: number;
    fullName: string;
    email: string;
    role: number;
  };
}

interface StudentResponse {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  cin: string;
  studentIdentifier: string;
  email: string;
  phone: string | null;
  fieldOfStudy: string;
  studyLevel: string;
  photoUrl: string | null;
  address: string | null;
}

interface InvoiceResponse {
  id: number;
  studentId: number;
  studentName: string;
  studentIdentifier: string;
  invoiceNumber: string;
  amount: number;
  issuedDate: string;
  status: 'PAID' | 'UNPAID';
  paymentMethod: string | null;
  paidDate: string | null;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);

  token = localStorage.getItem('academix_token') ?? '';
  query = '';
  loading = false;
  loadingStudents = false;
  passwordVisible = false;
  message = '';
  messageType: 'success' | 'error' = 'success';
  students: StudentResponse[] = [];
  invoices: InvoiceResponse[] = [];
  invoiceFilter: 'ALL' | 'PAID' | 'UNPAID' = 'ALL';
  editingInvoice: InvoiceResponse | null = null;
  invoicePendingDeletion: InvoiceResponse | null = null;
  invoiceStudentQuery = '';
  editInvoiceStudentQuery = '';
  invoicePendingPayment: InvoiceResponse | null = null;
  paymentMethod = '';
  editingStudent: StudentResponse | null = null;
  studentPendingDeletion: StudentResponse | null = null;

  loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  studentForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    cin: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    phone: [''],
    fieldOfStudy: ['', [Validators.required]],
    studyLevel: ['', [Validators.required]],
    photoUrl: [''],
    address: ['']
  });

  editForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    cin: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    fieldOfStudy: ['', [Validators.required]],
    studyLevel: ['', [Validators.required]],
    photoUrl: [''],
    address: ['']
  });

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
    if (this.token) {
      this.loadStudents();
      this.loadInvoices();
    }
  }

  get studentCount(): number {
    return this.students.length;
  }

  get filteredInvoices(): InvoiceResponse[] {
    return this.invoiceFilter === 'ALL'
      ? this.invoices
      : this.invoices.filter((invoice) => invoice.status === this.invoiceFilter);
  }

  get invoiceStudentSuggestions(): StudentResponse[] {
    return this.studentSuggestions(this.invoiceStudentQuery);
  }

  get editInvoiceStudentSuggestions(): StudentResponse[] {
    return this.studentSuggestions(this.editInvoiceStudentQuery);
  }

  get initials(): string {
    const email = this.loginForm.controls.email.value;
    return email ? email.slice(0, 2).toUpperCase() : 'AD';
  }

  login(): void {
    if (this.loginForm.invalid) {
      this.showMessage('Enter the admin email and password.', 'error');
      return;
    }

    this.loading = true;
    this.http.post<AuthResponse>('/api/auth/signin', this.loginForm.getRawValue()).subscribe({
      next: (response) => {
        this.token = response.token;
        localStorage.setItem('academix_token', response.token);
        this.showMessage(`Welcome ${response.user.fullName}.`, 'success');
        this.loadStudents();
        this.loadInvoices();
      },
      error: (error) => this.handleError(error, 'Login failed. Check the backend and credentials.'),
      complete: () => this.loading = false
    });
  }

  logout(): void {
    this.token = '';
    this.students = [];
    localStorage.removeItem('academix_token');
    this.showMessage('Signed out.', 'success');
  }

  createStudent(): void {
    if (!this.token) {
      this.showMessage('Sign in as admin first.', 'error');
      return;
    }
    if (this.studentForm.invalid) {
      this.showMessage('Fill all required student fields.', 'error');
      return;
    }

    this.loading = true;
    this.http.post<StudentResponse>('/api/students', this.studentForm.getRawValue(), {
      headers: this.authHeaders()
    }).subscribe({
      next: (student) => {
        this.showMessage(`Created student ${student.firstName} ${student.lastName}.`, 'success');
        this.studentForm.reset();
        this.loadStudents();
      },
      error: (error) => this.handleError(error, 'Could not create student.'),
      complete: () => this.loading = false
    });
  }

  loadStudents(): void {
    if (!this.token) {
      return;
    }

    this.loadingStudents = true;
    this.http.get<StudentResponse[]>('/api/students', {
      headers: this.authHeaders()
    }).subscribe({
      next: (students) => this.students = students,
      error: (error) => this.handleError(error, 'Could not load students.'),
      complete: () => this.loadingStudents = false
    });
  }

  searchStudents(): void {
    if (!this.token) {
      return;
    }

    const url = this.query.trim()
      ? `/api/students/search?query=${encodeURIComponent(this.query.trim())}`
      : '/api/students';

    this.loadingStudents = true;
    this.http.get<StudentResponse[]>(url, {
      headers: this.authHeaders()
    }).subscribe({
      next: (students) => this.students = students,
      error: (error) => this.handleError(error, 'Search failed.'),
      complete: () => this.loadingStudents = false
    });
  }

  createInvoice(): void {
    if (this.invoiceForm.invalid) {
      this.showMessage('Select a student and enter a valid amount.', 'error');
      return;
    }
    const value = this.invoiceForm.getRawValue();
    this.loading = true;
    this.http.post<InvoiceResponse>('/api/invoices', {
      ...value,
      paymentMethod: value.paymentMethod || null
    }, { headers: this.authHeaders() }).subscribe({
      next: (invoice) => {
        this.invoices = [invoice, ...this.invoices];
        this.showMessage(`Created invoice ${invoice.invoiceNumber}.`, 'success');
        this.invoiceForm.reset({ studentId: 0, amount: 0, issuedDate: new Date().toISOString().slice(0, 10), status: 'UNPAID', paymentMethod: '' });
        this.invoiceStudentQuery = '';
      },
      error: (error) => this.handleError(error, 'Could not create invoice.'),
      complete: () => this.loading = false
    });
  }

  loadInvoices(): void {
    if (!this.token) return;
    this.http.get<InvoiceResponse[]>('/api/invoices', { headers: this.authHeaders() }).subscribe({
      next: (invoices) => this.invoices = invoices,
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
      this.showMessage('Complete the required invoice details.', 'error');
      return;
    }
    const value = this.editInvoiceForm.getRawValue();
    this.loading = true;
    this.http.put<InvoiceResponse>(`/api/invoices/${this.editingInvoice.id}`, {
      ...value,
      paymentMethod: value.paymentMethod || null,
      paidDate: value.paidDate || null
    }, { headers: this.authHeaders() }).subscribe({
      next: (invoice) => {
        this.invoices = this.invoices.map((item) => item.id === invoice.id ? invoice : item);
        this.editingInvoice = null;
        this.showMessage(`Updated invoice ${invoice.invoiceNumber}.`, 'success');
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
      this.showMessage('Select a payment method.', 'error');
      return;
    }
    this.loading = true;
    this.http.put<InvoiceResponse>(`/api/invoices/${invoice.id}`, {
      studentId: invoice.studentId, amount: invoice.amount, issuedDate: invoice.issuedDate,
      status: 'PAID', paymentMethod: this.paymentMethod, paidDate: new Date().toISOString().slice(0, 10)
    }, { headers: this.authHeaders() }).subscribe({
      next: (updated) => {
        this.invoices = this.invoices.map((item) => item.id === updated.id ? updated : item);
        this.invoicePendingPayment = null;
        this.showMessage(`Marked ${updated.invoiceNumber} as paid.`, 'success');
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
    this.http.delete<void>(`/api/invoices/${invoice.id}`, { headers: this.authHeaders() }).subscribe({
      next: () => {
        this.invoices = this.invoices.filter((item) => item.id !== invoice.id);
        this.invoicePendingDeletion = null;
        this.showMessage(`Deleted invoice ${invoice.invoiceNumber}.`, 'success');
      },
      error: (error) => this.handleError(error, 'Could not delete invoice.'),
      complete: () => this.loading = false
    });
  }

  printInvoice(invoice: InvoiceResponse): void {
    const printWindow = window.open('', '_blank', 'width=720,height=640');
    if (!printWindow) {
      this.showMessage('Allow pop-ups to print this invoice.', 'error');
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

  private studentSuggestions(query: string): StudentResponse[] {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) return [];
    return this.students.filter((student) =>
      `${student.firstName} ${student.lastName} ${student.studentIdentifier}`.toLowerCase().includes(normalizedQuery)
    ).slice(0, 6);
  }

  trackStudent(_: number, student: StudentResponse): number {
    return student.id;
  }

  startEditing(student: StudentResponse): void {
    this.editingStudent = student;
    this.editForm.reset({
      firstName: student.firstName,
      lastName: student.lastName,
      cin: student.cin,
      email: student.email,
      phone: student.phone ?? '',
      fieldOfStudy: student.fieldOfStudy,
      studyLevel: student.studyLevel,
      photoUrl: student.photoUrl ?? '',
      address: student.address ?? ''
    });
  }

  cancelEditing(): void {
    this.editingStudent = null;
  }

  updateStudent(): void {
    if (!this.editingStudent || this.editForm.invalid) {
      this.showMessage('Fill all required student fields.', 'error');
      return;
    }

    const student = this.editingStudent;
    this.loading = true;
    this.http.put<StudentResponse>(`/api/students/${student.id}`, {
      ...this.editForm.getRawValue(),
      studentIdentifier: student.studentIdentifier
    }, { headers: this.authHeaders() }).subscribe({
      next: (updatedStudent) => {
        this.students = this.students.map((item) => item.id === updatedStudent.id ? updatedStudent : item);
        this.showMessage(`Updated ${updatedStudent.firstName} ${updatedStudent.lastName}.`, 'success');
        this.editingStudent = null;
      },
      error: (error) => this.handleError(error, 'Could not update student.'),
      complete: () => this.loading = false
    });
  }

  requestDelete(student: StudentResponse): void {
    this.studentPendingDeletion = student;
  }

  cancelDelete(): void {
    this.studentPendingDeletion = null;
  }

  deleteStudent(): void {
    const student = this.studentPendingDeletion;
    if (!student) {
      return;
    }

    this.loading = true;
    this.http.delete<void>(`/api/students/${student.id}`, { headers: this.authHeaders() }).subscribe({
      next: () => {
        this.students = this.students.filter((item) => item.id !== student.id);
        this.showMessage(`Deleted ${student.firstName} ${student.lastName}.`, 'success');
        this.studentPendingDeletion = null;
      },
      error: (error) => this.handleError(error, 'Could not delete student.'),
      complete: () => this.loading = false
    });
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      Authorization: `Bearer ${this.token}`
    });
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    const message = this.extractErrorMessage(error) || fallback;
    this.showMessage(message, 'error');
  }

  private extractErrorMessage(error: unknown): string {
    if (typeof error !== 'object' || error === null || !('error' in error)) {
      return '';
    }

    const body = (error as { error?: unknown }).error;
    if (typeof body === 'object' && body !== null && 'message' in body) {
      return String((body as { message?: unknown }).message);
    }
    if (typeof body === 'string') {
      return body;
    }
    return '';
  }

  private showMessage(message: string, type: 'success' | 'error'): void {
    this.message = message;
    this.messageType = type;
  }
}
