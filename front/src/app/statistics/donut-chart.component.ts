import { CommonModule } from '@angular/common';
import { Component, ElementRef, Input, ViewChild, computed, signal } from '@angular/core';

export interface DonutSegmentInput {
  label: string;
  value: number;
  color: string;
}

interface DonutSegment extends DonutSegmentInput {
  pct: number;
  dashArray: string;
  dashOffset: number;
}

interface DonutTooltip {
  x: number;
  y: number;
  label: string;
  value: number;
  pct: number;
}

const SIZE = 160;
const STROKE = 20;
const RADIUS = (SIZE - STROKE) / 2;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;
const SEGMENT_GAP = 3;

/** Hand-rolled donut (stroke-dasharray technique) — part-to-whole categorical
 * breakdowns only; reused wherever a few named categories share a total. */
@Component({
  selector: 'app-donut-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './donut-chart.component.html',
  styleUrl: './donut-chart.component.css'
})
export class DonutChartComponent {
  @ViewChild('wrap', { static: true }) private wrapRef!: ElementRef<HTMLDivElement>;

  @Input() centerLabel = '';
  @Input() set data(value: DonutSegmentInput[] | null | undefined) {
    this.rawData.set(value ?? []);
  }

  readonly size = SIZE;
  readonly strokeWidth = STROKE;
  readonly radius = RADIUS;

  private readonly rawData = signal<DonutSegmentInput[]>([]);
  readonly hovered = signal<number | null>(null);
  readonly tooltip = signal<DonutTooltip | null>(null);

  readonly total = computed(() => this.rawData().reduce((sum, entry) => sum + entry.value, 0));

  readonly segments = computed<DonutSegment[]>(() => {
    const total = this.total();
    if (!total) return [];
    let cumulative = 0;
    return this.rawData().map((entry) => {
      const raw = (entry.value / total) * CIRCUMFERENCE;
      const effective = Math.max(raw - SEGMENT_GAP, 0);
      const segment: DonutSegment = {
        ...entry,
        pct: (entry.value / total) * 100,
        dashArray: `${effective} ${CIRCUMFERENCE - effective}`,
        dashOffset: -cumulative
      };
      cumulative += raw;
      return segment;
    });
  });

  onEnter(event: PointerEvent, index: number): void {
    this.hovered.set(index);
    this.moveTooltip(event, index);
  }

  onMove(event: PointerEvent, index: number): void {
    this.moveTooltip(event, index);
  }

  onFocus(index: number): void {
    this.hovered.set(index);
    const rect = this.wrapRef.nativeElement.getBoundingClientRect();
    this.setTooltip(rect.width / 2, 6, index);
  }

  onLeave(): void {
    this.hovered.set(null);
    this.tooltip.set(null);
  }

  private moveTooltip(event: PointerEvent, index: number): void {
    const rect = this.wrapRef.nativeElement.getBoundingClientRect();
    this.setTooltip(event.clientX - rect.left, event.clientY - rect.top, index);
  }

  private setTooltip(x: number, y: number, index: number): void {
    const segment = this.segments()[index];
    if (!segment) return;
    this.tooltip.set({ x, y, label: segment.label, value: segment.value, pct: segment.pct });
  }
}
