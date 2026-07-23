import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { InvoiceResponse } from './invoice.model';

@Injectable({ providedIn: 'root' })
export class InvoicesService {
  private readonly http = inject(HttpClient);

  private readonly invoicesSignal = signal<InvoiceResponse[]>([]);
  readonly invoices = this.invoicesSignal.asReadonly();

  load(): Observable<InvoiceResponse[]> {
    return this.http.get<InvoiceResponse[]>('/api/invoices').pipe(
      tap((invoices) => this.invoicesSignal.set(invoices))
    );
  }

  loadMine(): Observable<InvoiceResponse[]> {
    return this.http.get<InvoiceResponse[]>('/api/invoices/me').pipe(
      tap((invoices) => this.invoicesSignal.set(invoices))
    );
  }

  create(payload: Record<string, unknown>): Observable<InvoiceResponse> {
    return this.http.post<InvoiceResponse>('/api/invoices', payload).pipe(
      tap((invoice) => this.invoicesSignal.update((invoices) => [invoice, ...invoices]))
    );
  }

  update(id: number, payload: Record<string, unknown>): Observable<InvoiceResponse> {
    return this.http.put<InvoiceResponse>(`/api/invoices/${id}`, payload).pipe(
      tap((updated) => this.invoicesSignal.update((invoices) =>
        invoices.map((item) => item.id === updated.id ? updated : item)
      ))
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/invoices/${id}`).pipe(
      tap(() => this.invoicesSignal.update((invoices) => invoices.filter((item) => item.id !== id)))
    );
  }
}
