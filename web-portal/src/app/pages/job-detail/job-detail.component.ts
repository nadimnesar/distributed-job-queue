import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { JobService } from '../../services/job.service';
import { AlertService } from '../../services/alert.service';
import { Job } from '../../models/job.model';

@Component({
  selector: 'app-job-detail',
  imports: [CommonModule, RouterLink],
  template: `
    <a routerLink="/jobs" class="text-decoration-none mb-3 d-inline-block">
      &larr; Back to Jobs
    </a>

    @if (loading) {
      <div class="text-center py-5">
        <div class="spinner-border text-secondary" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
      </div>
    } @else if (job) {
      <div class="d-flex justify-content-between align-items-start mb-4">
        <div>
          <h2 class="fw-semibold mb-1">Job Detail</h2>
          <code class="text-muted">{{ job.id }}</code>
        </div>
        <div class="d-flex gap-2">
          @if (job.status === 'PENDING' || job.status === 'FAILED' || job.status === 'PROCESSING') {
            <button class="btn btn-outline-danger btn-sm" (click)="cancel()">Cancel Job</button>
          }
          @if (job.status === 'DEAD') {
            <button class="btn btn-outline-success btn-sm" (click)="revive()">Revive Job</button>
          }
        </div>
      </div>

      <div class="row g-4">
        <div class="col-md-8">
          <div class="card border-0 shadow-sm">
            <div class="card-body">
              <div class="row g-3">
                <div class="col-sm-6">
                  <label class="text-muted small d-block">Status</label>
                  <span class="status-badge status-{{ job.status }}">{{ job.status }}</span>
                </div>
                <div class="col-sm-6">
                  <label class="text-muted small d-block">Priority</label>
                  <span class="priority-{{ job.priority }}">{{ job.priority }}</span>
                </div>
                <div class="col-sm-6">
                  <label class="text-muted small d-block">Type</label>
                  <span>{{ formatType(job.type) }}</span>
                </div>
                <div class="col-sm-6">
                  <label class="text-muted small d-block">Attempts</label>
                  <span>{{ job.attemptCount ?? 0 }} / {{ job.maxAttemptCount ?? 5 }}</span>
                </div>
                <div class="col-12">
                  <label class="text-muted small d-block">Payload</label>
                  <pre class="bg-light p-3 rounded mb-0 small">{{ job.result || '—' }}</pre>
                </div>
                <div class="col-sm-6">
                  <label class="text-muted small d-block">Started At</label>
                  <span>{{ job.startedAt ? (job.startedAt | date:'medium') : '—' }}</span>
                </div>
                <div class="col-sm-6">
                  <label class="text-muted small d-block">Completed At</label>
                  <span>{{ job.completedAt ? (job.completedAt | date:'medium') : '—' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-md-4">
          <div class="card border-0 shadow-sm mb-3">
            <div class="card-body">
              <h6 class="card-title text-muted">Dependencies</h6>
              @if (job.dependencies.length === 0) {
                <p class="text-muted small mb-0">None</p>
              } @else {
                <ul class="list-unstyled mb-0">
                  @for (dep of job.dependencies; track dep) {
                    <li class="small font-monospace">
                      <a [routerLink]="['/jobs', dep]" class="text-decoration-none">{{ dep.substring(0, 8) }}...</a>
                    </li>
                  }
                </ul>
              }
            </div>
          </div>

          <div class="card border-0 shadow-sm">
            <div class="card-body">
              <h6 class="card-title text-muted">Dependents</h6>
              @if (job.dependents.length === 0) {
                <p class="text-muted small mb-0">None</p>
              } @else {
                <ul class="list-unstyled mb-0">
                  @for (dep of job.dependents; track dep) {
                    <li class="small font-monospace">
                      <a [routerLink]="['/jobs', dep]" class="text-decoration-none">{{ dep.substring(0, 8) }}...</a>
                    </li>
                  }
                </ul>
              }
            </div>
          </div>
        </div>
      </div>
    } @else {
      <div class="text-center py-5 text-muted">
        <p>Job not found.</p>
        <a routerLink="/jobs" class="btn btn-outline-secondary btn-sm">Back to Jobs</a>
      </div>
    }
  `
})
export class JobDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private jobService = inject(JobService);
  private alertService = inject(AlertService);

  job: Job | null = null;
  loading = true;

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.jobService.get(id).subscribe({
        next: (res) => { this.job = res.data; this.loading = false; },
        error: () => { this.loading = false; }
      });
    } else {
      this.loading = false;
    }
  }

  cancel() {
    if (!this.job) return;
    this.jobService.cancel(this.job.id).subscribe({
      next: () => {
        this.alertService.success('Job cancelled');
        this.job = { ...this.job!, status: 'CANCELED' };
      }
    });
  }

  revive() {
    if (!this.job) return;
    this.jobService.revive(this.job.id).subscribe({
      next: () => {
        this.alertService.success('Job revived');
        this.job = { ...this.job!, status: 'PENDING', attemptCount: 1 };
      }
    });
  }

  formatType(type: string): string {
    return type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }
}
