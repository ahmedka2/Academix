import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { NotificationService } from '../core/notification.service';
import { extractErrorMessage } from '../core/http-error.util';
import { CategoryCountResponse } from './statistics.model';
import { StatisticsService } from './statistics.service';
import { DonutSegmentInput, DonutChartComponent } from './donut-chart.component';

const PASS_RATE_THRESHOLD = 60;

// Validated categorical order (dataviz skill default) — fixed hue anchors,
// assigned by sorted label so a category keeps its color across reloads
// regardless of backend iteration order. Re-run scripts/validate_palette.js
// against this app's paper surface (#fffefc) before changing any of these.
const CATEGORICAL_COLORS = ['#2a78d6', '#008300', '#e87ba4', '#eda100', '#1baf7a', '#eb6834', '#4a3aa7', '#e34948'];

// Status tokens mirrored from styles.css (--success/--warning/--danger/--subtle)
// as concrete hex — SVG stroke attributes here bypass the cascade, so these
// must be kept in sync with the shared tokens by hand.
const STATUS_JUSTIFIED = '#3f6b4a';
const STATUS_PENDING = '#a5690e';
const STATUS_UNJUSTIFIED = '#a63a2e';
const STATUS_REJECTED = '#9b9078';

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [CommonModule, DonutChartComponent],
  templateUrl: './statistics.component.html',
  styleUrl: './statistics.component.css'
})
export class StatisticsComponent implements OnInit {
  private readonly statisticsService = inject(StatisticsService);
  private readonly notifications = inject(NotificationService);

  readonly statistics = this.statisticsService.statistics;
  loading = false;

  /** Flips true one frame after data lands, so bar widths transition in from 0
   * instead of appearing pre-filled — an entrance, not a per-refresh replay. */
  readonly revealed = signal(false);

  readonly passRateHealthy = computed(() => (this.statistics()?.overallPassRate ?? 0) >= PASS_RATE_THRESHOLD);

  readonly fieldOfStudySegments = computed<DonutSegmentInput[]>(() =>
    this.toDonutSegments(this.statistics()?.studentsByFieldOfStudy)
  );
  readonly levelSegments = computed<DonutSegmentInput[]>(() =>
    this.toDonutSegments(this.statistics()?.studentsByLevel)
  );

  readonly absencesSegments = computed<DonutSegmentInput[]>(() => {
    const a = this.statistics()?.absences;
    if (!a || !a.total) return [];
    return [
      { label: 'Justified', value: a.justified, color: STATUS_JUSTIFIED },
      { label: 'Pending review', value: a.pendingJustification, color: STATUS_PENDING },
      { label: 'Unjustified', value: a.unjustified, color: STATUS_UNJUSTIFIED },
      { label: 'Rejected', value: a.rejected, color: STATUS_REJECTED }
    ].filter((entry) => entry.value > 0);
  });

  readonly paidPct = computed(() => {
    const p = this.statistics()?.payments;
    return p && p.totalInvoices ? (p.paidCount / p.totalInvoices) * 100 : 0;
  });

  ngOnInit(): void {
    this.loadStatistics();
  }

  loadStatistics(): void {
    this.loading = true;
    this.statisticsService.load().subscribe({
      error: (error) => this.handleError(error, 'Could not load statistics.'),
      complete: () => {
        this.loading = false;
        requestAnimationFrame(() => this.revealed.set(true));
      }
    });
  }

  barWidth(value: number, max: number): number {
    return max > 0 ? Math.max((value / max) * 100, value > 0 ? 4 : 0) : 0;
  }

  private toDonutSegments(categories: CategoryCountResponse[] | undefined): DonutSegmentInput[] {
    if (!categories?.length) return [];
    const sorted = [...categories].sort((a, b) => a.label.localeCompare(b.label));
    const maxSlots = CATEGORICAL_COLORS.length;
    if (sorted.length <= maxSlots) {
      return sorted.map((entry, i) => ({ label: entry.label, value: entry.count, color: CATEGORICAL_COLORS[i] }));
    }
    const head = sorted.slice(0, maxSlots - 1);
    const tail = sorted.slice(maxSlots - 1);
    const otherCount = tail.reduce((sum, entry) => sum + entry.count, 0);
    return [
      ...head.map((entry, i) => ({ label: entry.label, value: entry.count, color: CATEGORICAL_COLORS[i] })),
      { label: 'Other', value: otherCount, color: CATEGORICAL_COLORS[maxSlots - 1] }
    ];
  }

  private handleError(error: unknown, fallback: string): void {
    this.loading = false;
    const message = extractErrorMessage(error) || fallback;
    this.notifications.show(message, 'error');
  }
}
