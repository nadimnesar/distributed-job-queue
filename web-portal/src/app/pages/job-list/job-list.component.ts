import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { debounceTime } from 'rxjs';
import { JobService } from '../../services/job.service';
import { AlertService } from '../../services/alert.service';
import { Job, JobStatus, JobType, JOB_STATUSES, JOB_TYPES } from '../../models/job.model';
import { JobCreateModalComponent } from '../../components/job-create-modal/job-create-modal.component';

@Component({
  selector: 'app-job-list',
  imports: [CommonModule, RouterLink, ReactiveFormsModule, JobCreateModalComponent],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="mb-0 fw-semibold">Jobs</h2>
      <div class="d-flex gap-2">
        <button class="btn btn-outline-danger btn-sm" (click)="reviveAll()" [disabled]="loading">
          Revive All Dead
        </button>
        <button class="btn btn-primary btn-sm" (click)="showCreateModal = true">Create Job</button>
      </div>
    </div>

    <div class="card border-0 shadow-sm mb-3">
      <div class="card-body py-2">
        <form [formGroup]="filterForm" class="row g-2 align-items-end">
          <div class="col-auto">
            <label class="form-label small text-muted mb-0">Status</label>
            <select class="form-select form-select-sm" formControlName="status">
              <option value="">All</option>
              @for (s of statuses; track s) {
                <option [value]="s">{{ s }}</option>
              }
            </select>
          </div>
          <div class="col-auto">
            <label class="form-label small text-muted mb-0">Type</label>
            <select class="form-select form-select-sm" formControlName="type">
              <option value="">All</option>
              @for (t of types; track t) {
                <option [value]="t">{{ formatType(t) }}</option>
              }
            </select>
          </div>
          <div class="col-auto">
            <button type="button" class="btn btn-outline-secondary btn-sm" (click)="clearFilters()">Clear</button>
          </div>
        </form>
      </div>
    </div>

    @if (loading) {
      <div class="text-center py-5">
        <div class="spinner-border text-secondary" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
      </div>
    } @else if (jobs.length === 0) {
      <div class="text-center py-5 text-muted">
        <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" fill="currentColor" class="mb-3" viewBox="0 0 16 16">
          <path d="M4 1.5H3a2 2 0 0 0-2 2V14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V3.5a2 2 0 0 0-2-2h-1v1h1a1 1 0 0 1 1 1V14a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V3.5a1 1 0 0 1 1-1h1v-1z"/>
          <path d="M9.5 1a.5.5 0 0 1 .5.5v1a.5.5 0 0 1-.5.5h-3a.5.5 0 0 1-.5-.5v-1a.5.5 0 0 1 .5-.5h3zm-3-1A1.5 1.5 0 0 0 5 1.5v1A1.5 1.5 0 0 0 6.5 4h3A1.5 1.5 0 0 0 11 2.5v-1A1.5 1.5 0 0 0 9.5 0h-3z"/>
        </svg>
        <p class="mb-0">No jobs found</p>
      </div>
    } @else {
      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead class="table-light">
            <tr>
              <th style="width: 280px">ID</th>
              <th>Type</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Attempts</th>
              <th>Started</th>
              <th>Completed</th>
              <th style="width: 160px">Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (job of jobs; track job.id) {
              <tr>
                <td>
                  <a [routerLink]="['/jobs', job.id]" class="text-decoration-none font-monospace small">
                    {{ job.id.substring(0, 8) }}...
                  </a>
                </td>
                <td>{{ formatType(job.type) }}</td>
                <td>
                  <span class="status-badge status-{{ job.status }}">{{ job.status }}</span>
                </td>
                <td>
                  <span class="priority-{{ job.priority }}">{{ job.priority }}</span>
                </td>
                <td>{{ job.attemptCount ?? 0 }} / {{ job.maxAttemptCount ?? 5 }}</td>
                <td class="small text-muted">{{ job.startedAt ? (job.startedAt | date:'short') : '—' }}</td>
                <td class="small text-muted">{{ job.completedAt ? (job.completedAt | date:'short') : '—' }}</td>
                <td>
                  <div class="btn-group btn-group-sm">
                    <button class="btn btn-outline-danger"
                            [disabled]="job.status !== 'PENDING' && job.status !== 'FAILED' && job.status !== 'PROCESSING'"
                            (click)="cancelJob(job)">
                      Cancel
                    </button>
                    <button class="btn btn-outline-success"
                            [disabled]="job.status !== 'DEAD'"
                            (click)="reviveJob(job)">
                      Revive
                    </button>
                  </div>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <nav class="d-flex justify-content-between align-items-center mt-3">
        <small class="text-muted">Page {{ page + 1 }}</small>
        <div class="btn-group btn-group-sm">
          <button class="btn btn-outline-secondary" [disabled]="page === 0" (click)="goPage(page - 1)">Previous</button>
          <button class="btn btn-outline-secondary" [disabled]="jobs.length < pageSize" (click)="goPage(page + 1)">Next</button>
        </div>
      </nav>
    }

    <app-job-create-modal
      [isOpen]="showCreateModal"
      (close)="showCreateModal = false"
      (jobCreated)="onJobCreated()">
    </app-job-create-modal>
  `
})
export class JobListComponent implements OnInit {
  private jobService = inject(JobService);
  private alertService = inject(AlertService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  jobs: Job[] = [];
  loading = true;
  page = 0;
  pageSize = 20;
  showCreateModal = false;

  statuses = JOB_STATUSES;
  types = JOB_TYPES;

  filterForm: FormGroup = this.fb.group({
    status: [''],
    type: ['']
  });

  ngOnInit() {
    // Restore filters from URL query params
    const qp = this.route.snapshot.queryParamMap;
    this.filterForm.patchValue({
      status: qp.get('status') || '',
      type: qp.get('type') || ''
    });

    // Debounce filter changes
    this.filterForm.valueChanges.pipe(debounceTime(300)).subscribe(() => {
      this.page = 0;
      this.updateUrlAndLoad();
    });

    this.loadJobs();
  }

  updateUrlAndLoad() {
    const v = this.filterForm.value;
    const queryParams: any = {};
    if (v.status) queryParams.status = v.status;
    if (v.type) queryParams.type = v.type;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge'
    });

    this.loadJobs();
  }

  loadJobs() {
    this.loading = true;
    const v = this.filterForm.value;

    const observable = (v.status || v.type)
      ? this.jobService.filter(v.status || undefined, v.type || undefined, this.page, this.pageSize)
      : this.jobService.list(this.page, this.pageSize);

    observable.subscribe({
      next: (res) => {
        this.jobs = res.data ?? [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  clearFilters() {
    this.filterForm.patchValue({ status: '', type: '' });
  }

  goPage(p: number) {
    this.page = p;
    this.loadJobs();
  }

  cancelJob(job: Job) {
    this.jobService.cancel(job.id).subscribe({
      next: () => {
        this.alertService.success('Job cancelled successfully');
        this.loadJobs();
      }
    });
  }

  reviveJob(job: Job) {
    this.jobService.revive(job.id).subscribe({
      next: () => {
        this.alertService.success('Job revived successfully');
        this.loadJobs();
      }
    });
  }

  reviveAll() {
    this.jobService.reviveAll().subscribe({
      next: (res) => {
        const count = res.data?.length ?? 0;
        this.alertService.success(`${count} dead job(s) revived`);
        this.loadJobs();
      }
    });
  }

  onJobCreated() {
    this.showCreateModal = false;
    this.loadJobs();
  }

  formatType(type: string): string {
    return type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }
}
