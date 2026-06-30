import { Component, input, output, inject, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { JobService } from '../../services/job.service';
import { AlertService } from '../../services/alert.service';
import { JobPriority, JobType, JOB_PRIORITIES, JOB_TYPES } from '../../models/job.model';
import { formatType as formatTypeUtil } from '../../utils/format.utils';

@Component({
  selector: 'app-job-create-modal',
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    @if (isOpen()) {
      <div class="modal fade show d-block" tabindex="-1"
           role="dialog"
           aria-modal="true"
           aria-labelledby="job-create-modal-title"
           (click)="onBackdropClick($event)">
        <div class="modal-dialog" role="document" (click)="$event.stopPropagation()">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title" id="job-create-modal-title">Create Job</h5>
              <button type="button" class="btn-close" aria-label="Close" (click)="close.emit()"></button>
            </div>
            <div class="modal-body">
              <form [formGroup]="form">
                <div class="row g-3">
                  <div class="col-md-6">
                    <label for="priority" class="form-label">Priority *</label>
                    <select id="priority" class="form-select" formControlName="priority"
                            aria-describedby="priority-help">
                      <option value="">Select priority</option>
                      @for (p of priorities; track p) {
                        <option [value]="p">{{ p }}</option>
                      }
                    </select>
                    <div id="priority-help" class="form-text">Required</div>
                  </div>

                  <div class="col-md-6">
                    <label for="type" class="form-label">Type *</label>
                    <select id="type" class="form-select" formControlName="type"
                            aria-describedby="type-help">
                      <option value="">Select type</option>
                      @for (t of types; track t) {
                        <option [value]="t">{{ formatType(t) }}</option>
                      }
                    </select>
                    <div id="type-help" class="form-text">Required</div>
                  </div>

                  <div class="col-12">
                    <label for="payload" class="form-label">Payload *</label>
                    <textarea id="payload" class="form-control" rows="4" formControlName="payload"
                              placeholder="Job payload content..."
                              [class.is-invalid]="form.get('payload')?.touched && form.get('payload')?.invalid"
                              aria-describedby="payload-error"></textarea>
                    <div id="payload-error" class="invalid-feedback">Payload is required (max 10,000 characters).</div>
                  </div>

                  <div class="col-md-6">
                    <label for="maxAttemptCount" class="form-label">Max Attempt Count</label>
                    <input id="maxAttemptCount" type="number" class="form-control" formControlName="maxAttemptCount"
                           min="1" placeholder="Default: 5">
                  </div>

                  <div class="col-12">
                    <label for="dependenciesRaw" class="form-label">Dependencies</label>
                    <textarea id="dependenciesRaw" class="form-control" rows="2" formControlName="dependenciesRaw"
                              placeholder="Comma-separated UUIDs (optional)"></textarea>
                    <div class="form-text">Enter UUIDs separated by commas.</div>
                  </div>
                </div>
              </form>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-outline-secondary" (click)="close.emit()">Cancel</button>
              <button type="button" class="btn btn-primary" [disabled]="form.invalid || submitting()"
                      (click)="submit()">
                @if (submitting()) {
                  <span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                  <span class="visually-hidden">Loading...</span>
                }
                Create Job
              </button>
            </div>
          </div>
        </div>
      </div>
      <div class="modal-backdrop fade show"></div>
    }
  `
})
export class JobCreateModalComponent {
  private fb = inject(FormBuilder);
  private jobService = inject(JobService);
  private alertService = inject(AlertService);

  isOpen = input.required<boolean>();
  close = output<void>();
  jobCreated = output<string>();

  priorities = JOB_PRIORITIES;
  types = JOB_TYPES;
  submitting = signal(false);

  form = this.fb.group({
    priority: ['', Validators.required],
    type: ['', Validators.required],
    payload: ['', [Validators.required, Validators.maxLength(10000)]],
    maxAttemptCount: [null as number | null],
    dependenciesRaw: ['']
  });

  onBackdropClick(event: MouseEvent) {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      this.close.emit();
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey() {
    if (this.isOpen()) {
      this.close.emit();
    }
  }

  submit() {
    if (this.form.invalid) return;
    this.submitting.set(true);

    const v = this.form.value;
    const deps = v.dependenciesRaw
      ? v.dependenciesRaw.split(',').map((s: string) => s.trim()).filter((s: string) => s.length > 0)
      : [];

    this.jobService.create({
      priority: v.priority as JobPriority,
      type: v.type as JobType,
      payload: v.payload!,
      dependencies: deps.length > 0 ? deps : undefined,
      maxAttemptCount: v.maxAttemptCount ?? undefined
    }).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.form.reset();
        this.alertService.success('Job created successfully');
        this.jobCreated.emit(res.data.id);
        this.close.emit();
      },
      error: () => {
        this.submitting.set(false);
      }
    });
  }

  formatType(type: string): string {
    return formatTypeUtil(type);
  }

}
