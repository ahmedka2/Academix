import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { StatisticsResponse } from './statistics.model';

@Injectable({ providedIn: 'root' })
export class StatisticsService {
  private readonly http = inject(HttpClient);

  private readonly statisticsSignal = signal<StatisticsResponse | null>(null);
  readonly statistics = this.statisticsSignal.asReadonly();

  load(): Observable<StatisticsResponse> {
    return this.http.get<StatisticsResponse>('/api/statistics').pipe(
      tap((statistics) => this.statisticsSignal.set(statistics))
    );
  }
}
