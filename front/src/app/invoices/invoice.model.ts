export interface InvoiceResponse {
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
