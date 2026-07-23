import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { LevelResponse } from './level.model';

@Injectable({ providedIn: 'root' })
export class LevelsService {
  private readonly http = inject(HttpClient);

  private readonly levelsSignal = signal<LevelResponse[]>([]);
  readonly levels = this.levelsSignal.asReadonly();

  load(): Observable<LevelResponse[]> {
    return this.http.get<LevelResponse[]>('/api/levels').pipe(
      tap((levels) => this.levelsSignal.set(levels))
    );
  }

  create(name: string): Observable<LevelResponse> {
    return this.http.post<LevelResponse>('/api/levels', { name }).pipe(
      tap((level) => this.levelsSignal.update((levels) => [...levels, level]))
    );
  }

  update(id: number, name: string): Observable<LevelResponse> {
    return this.http.put<LevelResponse>(`/api/levels/${id}`, { name }).pipe(
      tap((updated) => this.levelsSignal.update((levels) =>
        levels.map((item) => item.id === updated.id ? updated : item)
      ))
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/levels/${id}`).pipe(
      tap(() => this.levelsSignal.update((levels) => levels.filter((item) => item.id !== id)))
    );
  }
}
