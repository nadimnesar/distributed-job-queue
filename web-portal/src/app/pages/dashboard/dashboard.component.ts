import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../../services/dashboard.service';
import { JobsSummary, QueueMetrics } from '../../models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="mb-0 fw-semibold">Dashboard</h2>
      <button class="btn btn-outline-secondary btn-sm" (click)="load()" [disabled]="loading">
        Refresh
      </button>
    </div>

    @if (loading) {
      <div class="text-center py-5">
        <div class="spinner-border text-secondary" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
      </div>
    } @else {
      <h5 class="text-muted mb-3">Job Summary</h5>
      <div class="row g-3 mb-4">
        @for (item of summaryCards; track item.label) {
          <div class="col-md-3 col-lg-2">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body text-center py-3">
                <div class="fw-bold fs-3">{{ item.value }}</div>
                <div class="text-muted small text-uppercase">{{ item.label }}</div>
              </div>
            </div>
          </div>
        }
      </div>

      <h5 class="text-muted mb-3">Queue Metrics</h5>
      <div class="row g-3">
        @for (item of queueCards; track item.label) {
          <div class="col-md-4 col-lg">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body text-center py-3">
                <div class="fw-bold fs-4">{{ item.value }}</div>
                <div class="text-muted small text-uppercase">{{ item.label }}</div>
              </div>
            </div>
          </div>
        }
      </div>
    }
  `
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);

  loading = true;
  summary: JobsSummary | null = null;
  metrics: QueueMetrics | null = null;

  summaryCards: { label: string; value: number }[] = [];
  queueCards: { label: string; value: number }[] = [];

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    let loaded = 0;
    const check = () => { if (++loaded === 2) this.loading = false; };

    this.dashboardService.getJobSummary().subscribe({
      next: (res) => {
        this.summary = res.data;
        this.summaryCards = [
          { label: 'Total', value: res.data.TOTAL },
          { label: 'Pending', value: res.data.PENDING },
          { label: 'Processing', value: res.data.PROCESSING },
          { label: 'Completed', value: res.data.COMPLETED },
          { label: 'Failed', value: res.data.FAILED },
          { label: 'Dead', value: res.data.DEAD },
          { label: 'Canceled', value: res.data.CANCELED },
        ];
        check();
      },
      error: () => check()
    });

    this.dashboardService.getQueueMetrics().subscribe({
      next: (res) => {
        this.metrics = res.data;
        const q = res.data.queues;
        this.queueCards = [
          { label: 'High', value: q.HIGH_PRIORITY_QUEUE_LENGTH },
          { label: 'Medium', value: q.MEDIUM_PRIORITY_QUEUE_LENGTH },
          { label: 'Low', value: q.LOW_PRIORITY_QUEUE_LENGTH },
          { label: 'Retry', value: q.RETRY_QUEUE_LENGTH },
          { label: 'Dead Letter', value: q.DEAD_LETTER_QUEUE_LENGTH },
        ];
        check();
      },
      error: () => check()
    });
  }
}
