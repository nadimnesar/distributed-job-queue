import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { DashboardComponent } from './dashboard.component';
import { DashboardService } from '../../services/dashboard.service';
import { JobService } from '../../services/job.service';
import { AlertService } from '../../services/alert.service';
import { environment } from '../../../environments/environment';
import { formatType } from '../../utils/format.utils';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let dashboardService: DashboardService;
  let jobService: JobService;
  let httpMock: HttpTestingController;
  const dashboardUrl = `${environment.apiUrl}/v1/dashboard`;
  const jobsUrl = `${environment.apiUrl}/v1`;

  const mockJobs = [
    {
      id: 'job-001-abc', priority: 'HIGH', type: 'EMAIL_SENDING', status: 'PENDING',
      dependents: [], dependencies: [], attemptCount: 0, maxAttemptCount: 5
    },
    {
      id: 'job-002-def', priority: 'LOW', type: 'PAYMENT_SENDING', status: 'COMPLETED',
      dependents: [], dependencies: [], attemptCount: 3, maxAttemptCount: 5,
      startedAt: '2025-01-01T10:00:00Z', completedAt: '2025-01-01T10:05:00Z'
    },
    {
      id: 'job-003-ghi', priority: 'MEDIUM', type: 'EMAIL_SENDING', status: 'DEAD',
      dependents: [], dependencies: [], attemptCount: 5, maxAttemptCount: 5
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent, RouterTestingModule, ReactiveFormsModule],
      providers: [
        DashboardService,
        JobService,
        AlertService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    dashboardService = TestBed.inject(DashboardService);
    jobService = TestBed.inject(JobService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  /** Helper: flush the 3 parallel requests made on init (summary, metrics, jobs) */
  function flushInitialRequests(jobData: any[] = []) {
    const summaryReq = httpMock.expectOne(r => r.url === `${dashboardUrl}/jobs/summary`);
    summaryReq.flush({
      message: 'ok', code: 200,
      data: { TOTAL: 50, PENDING: 10, PROCESSING: 5, COMPLETED: 25, FAILED: 5, DEAD: 3, CANCELED: 2 }
    });

    const metricsReq = httpMock.expectOne(r => r.url === `${dashboardUrl}/queue-metrics`);
    metricsReq.flush({
      message: 'ok', code: 200,
      data: {
        queues: {
          HIGH_PRIORITY_QUEUE_LENGTH: 3,
          MEDIUM_PRIORITY_QUEUE_LENGTH: 2,
          LOW_PRIORITY_QUEUE_LENGTH: 1,
          DELAY_QUEUE_LENGTH: 0,
          RETRY_QUEUE_LENGTH: 1,
          DEAD_LETTER_QUEUE_LENGTH: 0
        }
      }
    });

    const jobsReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs`);
    jobsReq.flush({ message: 'ok', code: 200, data: jobData });
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Dashboard shows job summary and queue metrics', () => {
    it('renders summary cards after data loads', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cards = el.querySelectorAll('.card');
      expect(cards.length).toBeGreaterThanOrEqual(7); // 7 summary + 5 queue

      expect(el.textContent).toContain('50');
      expect(el.textContent).toContain('Pending');
      expect(el.textContent).toContain('Processing');
    });

    it('renders queue metrics section', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('Queue Metrics');
      expect(el.textContent).toContain('High');
      expect(el.textContent).toContain('Dead Letter');
    });
  });

  describe('loading state', () => {
    it('shows spinner while stats are loading', () => {
      fixture.detectChanges();
      const el = fixture.nativeElement;
      const spinner = el.querySelector('.spinner-border');
      expect(spinner).toBeTruthy();

      // Flush pending requests so afterEach verify() doesn't fail
      httpMock.match(r => r.url === `${dashboardUrl}/jobs/summary`).forEach(r =>
        r.flush({ message: 'ok', code: 200, data: { TOTAL: 0, PENDING: 0, PROCESSING: 0, COMPLETED: 0, FAILED: 0, DEAD: 0, CANCELED: 0 } })
      );
      httpMock.match(r => r.url === `${dashboardUrl}/queue-metrics`).forEach(r =>
        r.flush({ message: 'ok', code: 200, data: { queues: { HIGH_PRIORITY_QUEUE_LENGTH: 0, MEDIUM_PRIORITY_QUEUE_LENGTH: 0, LOW_PRIORITY_QUEUE_LENGTH: 0, DELAY_QUEUE_LENGTH: 0, RETRY_QUEUE_LENGTH: 0, DEAD_LETTER_QUEUE_LENGTH: 0 } } })
      );
      httpMock.match(r => r.url === `${jobsUrl}/jobs`).forEach(r =>
        r.flush({ message: 'ok', code: 200, data: [] })
      );
    });

    it('hides stats spinner after data loads', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const spinners = el.querySelectorAll('.spinner-border');
      expect(spinners.length).toBe(0);
    });
  });

  describe('refresh button', () => {
    it('has a refresh button', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const btns = Array.from(el.querySelectorAll('button')).filter(
        (b: any) => b.textContent.includes('Refresh')
      );
      expect(btns.length).toBe(1);
    });
  });

  describe('error handling', () => {
    it('stops loading even if API fails', () => {
      fixture.detectChanges();

      const summaryReq = httpMock.expectOne(r => r.url === `${dashboardUrl}/jobs/summary`);
      summaryReq.error(new ProgressEvent('error'));

      const metricsReq = httpMock.expectOne(r => r.url === `${dashboardUrl}/queue-metrics`);
      metricsReq.error(new ProgressEvent('error'));

      const jobsReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs`);
      jobsReq.error(new ProgressEvent('error'));

      fixture.detectChanges();

      expect(component.statsLoading).toBeFalse();
      expect(component.jobsLoading).toBeFalse();
    });
  });

  describe('Jobs heading', () => {
    it('shows Jobs heading between stats and table', () => {
      fixture.detectChanges();
      flushInitialRequests(mockJobs);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const headings = Array.from(el.querySelectorAll('h4')).filter(
        (h: any) => h.textContent.includes('Jobs')
      );
      expect(headings.length).toBe(1);
    });
  });

  describe('Job table rendering', () => {
    it('renders a table with column headers', () => {
      fixture.detectChanges();
      flushInitialRequests(mockJobs);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const headers = el.querySelectorAll('th');
      const headerTexts = Array.from(headers).map((h: any) => h.textContent.trim());
      expect(headerTexts).toContain('ID');
      expect(headerTexts).toContain('Type');
      expect(headerTexts).toContain('Status');
      expect(headerTexts).toContain('Priority');
    });

    it('renders job rows', () => {
      fixture.detectChanges();
      flushInitialRequests(mockJobs);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const rows = el.querySelectorAll('tbody tr');
      expect(rows.length).toBe(3);
    });

    it('displays truncated job ID', () => {
      fixture.detectChanges();
      flushInitialRequests(mockJobs);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('job-001');
    });

    it('displays job type formatted', () => {
      fixture.detectChanges();
      flushInitialRequests(mockJobs);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('Email Sending');
      expect(el.textContent).toContain('Payment Sending');
    });

    it('job IDs link to /jobs/:id', () => {
      fixture.detectChanges();
      flushInitialRequests(mockJobs);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const links = el.querySelectorAll('a[href]');
      const jobLinks = Array.from(links).filter((l: any) =>
        l.getAttribute('href')?.includes('/jobs/job-')
      );
      expect(jobLinks.length).toBe(3);
    });
  });

  describe('Pagination', () => {
    it('displays pagination controls', () => {
      fixture.detectChanges();
      flushInitialRequests(mockJobs);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('Page 1');
      expect(el.textContent).toContain('Previous');
      expect(el.textContent).toContain('Next');
    });

    it('disables Previous on first page', () => {
      fixture.detectChanges();
      flushInitialRequests(mockJobs);
      fixture.detectChanges();

      expect(component.page).toBe(0);
      const el = fixture.nativeElement;
      const prevBtn = Array.from(el.querySelectorAll('button')).find(
        (b: any) => b.textContent.includes('Previous')
      ) as any;
      expect(prevBtn.disabled).toBeTrue();
    });

    it('calls goPage to navigate pages', () => {
      fixture.detectChanges();
      flushInitialRequests(mockJobs);
      fixture.detectChanges();

      component.goPage(1);

      expect(component.page).toBe(1);
      const reloadReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs`);
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });
  });

  describe('Filtering', () => {
    it('renders filter form with status and type selects', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const selects = el.querySelectorAll('select');
      expect(selects.length).toBe(2);
    });

    it('populates status select with all job statuses', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const statusSelect = el.querySelectorAll('select')[0];
      const options = Array.from(statusSelect.querySelectorAll('option')).map((o: any) => o.value);
      expect(options).toContain('PENDING');
      expect(options).toContain('PROCESSING');
      expect(options).toContain('CANCELED');
      expect(options).toContain('COMPLETED');
      expect(options).toContain('FAILED');
      expect(options).toContain('DEAD');
    });

    it('populates type select with all job types', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const typeSelect = el.querySelectorAll('select')[1];
      const options = Array.from(typeSelect.querySelectorAll('option')).map((o: any) => o.value);
      expect(options).toContain('EMAIL_SENDING');
      expect(options).toContain('PAYMENT_SENDING');
    });

    it('calls filter endpoint when status is selected', fakeAsync(() => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      component.filterForm.patchValue({ status: 'PENDING', type: '' });
      tick(400);
      fixture.detectChanges();

      const filterReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs/filter`);
      expect(filterReq.request.params.get('status')).toBe('PENDING');
      filterReq.flush({ message: 'ok', code: 200, data: [] });
    }));

    it('calls filter endpoint when type is selected', fakeAsync(() => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      component.filterForm.patchValue({ status: '', type: 'EMAIL_SENDING' });
      tick(400);
      fixture.detectChanges();

      const filterReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs/filter`);
      expect(filterReq.request.params.get('type')).toBe('EMAIL_SENDING');
      filterReq.flush({ message: 'ok', code: 200, data: [] });
    }));

    it('clearFilters resets both form controls', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      component.filterForm.patchValue({ status: 'DEAD', type: 'PAYMENT_SENDING' });
      component.clearFilters();

      expect(component.filterForm.value.status).toBe('');
      expect(component.filterForm.value.type).toBe('');
    });
  });

  describe('Empty state', () => {
    it('displays no jobs message when list is empty', () => {
      fixture.detectChanges();
      flushInitialRequests([]);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('No jobs found');
    });

    it('does not show table when list is empty', () => {
      fixture.detectChanges();
      flushInitialRequests([]);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const table = el.querySelector('table');
      expect(table).toBeFalsy();
    });
  });

  describe('formatType', () => {
    it('formats underscored type names into title case', () => {
      expect(formatType('EMAIL_SENDING')).toBe('Email Sending');
      expect(formatType('PAYMENT_SENDING')).toBe('Payment Sending');
    });
  });

  describe('Header buttons', () => {
    it('has a Revive All Dead button', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const btns = Array.from(el.querySelectorAll('button')).filter(
        (b: any) => b.textContent.includes('Revive All')
      );
      expect(btns.length).toBe(1);
    });

    it('has a Create Job button', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const btns = Array.from(el.querySelectorAll('button')).filter(
        (b: any) => b.textContent.includes('Create Job')
      );
      expect(btns.length).toBe(1);
    });

    it('has a Refresh button', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const btns = Array.from(el.querySelectorAll('button')).filter(
        (b: any) => b.textContent.includes('Refresh')
      );
      expect(btns.length).toBe(1);
    });
  });

  describe('Job table actions column', () => {
    it('renders Actions column header', () => {
      fixture.detectChanges();
      flushInitialRequests(mockJobs);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const headers = el.querySelectorAll('th');
      const headerTexts = Array.from(headers).map((h: any) => h.textContent.trim());
      expect(headerTexts).toContain('Actions');
    });

    it('cancel button is enabled for PENDING jobs', () => {
      fixture.detectChanges();
      flushInitialRequests([mockJobs[0]]);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtns = el.querySelectorAll('table .btn-outline-danger');
      expect(cancelBtns.length).toBe(1);
      expect(cancelBtns[0].disabled).toBeFalse();
    });

    it('cancel button is enabled for FAILED jobs', () => {
      fixture.detectChanges();
      flushInitialRequests([{ ...mockJobs[0], status: 'FAILED' }]);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtns = el.querySelectorAll('table .btn-outline-danger');
      expect(cancelBtns.length).toBe(1);
      expect(cancelBtns[0].disabled).toBeFalse();
    });

    it('cancel button is enabled for PROCESSING jobs', () => {
      fixture.detectChanges();
      flushInitialRequests([{ ...mockJobs[0], status: 'PROCESSING' }]);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtns = el.querySelectorAll('table .btn-outline-danger');
      expect(cancelBtns.length).toBe(1);
      expect(cancelBtns[0].disabled).toBeFalse();
    });

    it('cancel button is disabled for CANCELED jobs', () => {
      fixture.detectChanges();
      flushInitialRequests([{ ...mockJobs[0], status: 'CANCELED' }]);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtns = el.querySelectorAll('table .btn-outline-danger');
      expect(cancelBtns[0].disabled).toBeTrue();
    });

    it('cancel button is disabled for COMPLETED jobs', () => {
      fixture.detectChanges();
      flushInitialRequests([{ ...mockJobs[0], status: 'COMPLETED' }]);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtns = el.querySelectorAll('table .btn-outline-danger');
      expect(cancelBtns[0].disabled).toBeTrue();
    });

    it('cancel button is disabled for DEAD jobs', () => {
      fixture.detectChanges();
      flushInitialRequests([{ ...mockJobs[0], status: 'DEAD' }]);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtns = el.querySelectorAll('table .btn-outline-danger');
      expect(cancelBtns[0].disabled).toBeTrue();
    });

    it('calls cancel endpoint when cancel is clicked', () => {
      fixture.detectChanges();
      flushInitialRequests([mockJobs[0]]);
      fixture.detectChanges();

      component.cancelJob(mockJobs[0] as any);

      const cancelReq = httpMock.expectOne(`${jobsUrl}/jobs/job-001-abc/cancel`);
      expect(cancelReq.request.method).toBe('POST');
      cancelReq.flush({ message: 'ok', code: 200, data: null });

      const reloadReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs`);
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });

    it('revive button is enabled for DEAD jobs', () => {
      fixture.detectChanges();
      flushInitialRequests([mockJobs[2]]);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const reviveBtns = el.querySelectorAll('table .btn-outline-success');
      expect(reviveBtns.length).toBe(1);
      expect(reviveBtns[0].disabled).toBeFalse();
    });

    it('revive button is disabled for PENDING jobs', () => {
      fixture.detectChanges();
      flushInitialRequests([mockJobs[0]]);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const reviveBtns = el.querySelectorAll('table .btn-outline-success');
      expect(reviveBtns[0].disabled).toBeTrue();
    });

    it('revive button is disabled for COMPLETED jobs', () => {
      fixture.detectChanges();
      flushInitialRequests([mockJobs[1]]);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const reviveBtns = el.querySelectorAll('table .btn-outline-success');
      expect(reviveBtns[0].disabled).toBeTrue();
    });

    it('calls revive endpoint when revive is clicked', () => {
      fixture.detectChanges();
      flushInitialRequests([mockJobs[2]]);
      fixture.detectChanges();

      component.reviveJob(mockJobs[2] as any);

      const reviveReq = httpMock.expectOne(`${jobsUrl}/jobs/job-003-ghi/revive`);
      expect(reviveReq.request.method).toBe('POST');
      reviveReq.flush({ message: 'ok', code: 200, data: 'revived' });

      const reloadReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs`);
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });
  });

  describe('Revive All Dead', () => {
    it('calls reviveAll endpoint when clicked', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      component.reviveAll();

      const reviveReq = httpMock.expectOne(`${jobsUrl}/jobs/revive`);
      expect(reviveReq.request.method).toBe('POST');
      reviveReq.flush({ message: 'ok', code: 200, data: ['j1', 'j2'] });

      const reloadReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs`);
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });
  });

  describe('AC3: Refresh reloads both stats and jobs', () => {
    it('refresh button click re-fetches stats and jobs', () => {
      fixture.detectChanges();
      flushInitialRequests(mockJobs);
      fixture.detectChanges();

      // Verify initial stats loaded
      expect(component.summaryCards.length).toBe(7);

      // Click refresh
      const el = fixture.nativeElement;
      const refreshBtn = Array.from(el.querySelectorAll('button')).find(
        (b: any) => b.textContent.includes('Refresh')
      ) as any;
      refreshBtn.click();

      // Expect new stats + jobs requests (expectOne throws if not found)
      const summaryReq = httpMock.expectOne(r => r.url === `${dashboardUrl}/jobs/summary`);
      summaryReq.flush({
        message: 'ok', code: 200,
        data: { TOTAL: 60, PENDING: 15, PROCESSING: 5, COMPLETED: 30, FAILED: 5, DEAD: 3, CANCELED: 2 }
      });

      const metricsReq = httpMock.expectOne(r => r.url === `${dashboardUrl}/queue-metrics`);
      metricsReq.flush({
        message: 'ok', code: 200,
        data: {
          queues: {
            HIGH_PRIORITY_QUEUE_LENGTH: 4,
            MEDIUM_PRIORITY_QUEUE_LENGTH: 3,
            LOW_PRIORITY_QUEUE_LENGTH: 2,
            DELAY_QUEUE_LENGTH: 0,
            RETRY_QUEUE_LENGTH: 1,
            DEAD_LETTER_QUEUE_LENGTH: 0
          }
        }
      });

      const jobsReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs`);
      jobsReq.flush({ message: 'ok', code: 200, data: [] });

      fixture.detectChanges();

      // Verify stats were refreshed with new data
      expect(component.summaryCards.find(c => c.label === 'Total')?.value).toBe(60);
    });
  });

  describe('AC4: Filters do not re-fetch dashboard stats', () => {
    it('changing status filter only calls jobs filter, not stats endpoints', fakeAsync(() => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      // Change filter
      component.filterForm.patchValue({ status: 'PENDING', type: '' });
      tick(400);
      fixture.detectChanges();

      // Stats endpoints should NOT have been called again — verify no pending requests to them
      const pendingStatsReq = httpMock.match(r => r.url === `${dashboardUrl}/jobs/summary`);
      expect(pendingStatsReq.length).toBe(0);

      const pendingMetricsReq = httpMock.match(r => r.url === `${dashboardUrl}/queue-metrics`);
      expect(pendingMetricsReq.length).toBe(0);

      // Should see filter request
      const filterReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs/filter`);
      expect(filterReq.request.params.get('status')).toBe('PENDING');
      filterReq.flush({ message: 'ok', code: 200, data: [] });
    }));

    it('changing type filter only calls jobs filter, not stats endpoints', fakeAsync(() => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      // Change filter
      component.filterForm.patchValue({ status: '', type: 'EMAIL_SENDING' });
      tick(400);
      fixture.detectChanges();

      // Stats endpoints should NOT have been called again
      const pendingStatsReq = httpMock.match(r => r.url === `${dashboardUrl}/jobs/summary`);
      expect(pendingStatsReq.length).toBe(0);

      const pendingMetricsReq = httpMock.match(r => r.url === `${dashboardUrl}/queue-metrics`);
      expect(pendingMetricsReq.length).toBe(0);

      // Should see filter request
      const filterReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs/filter`);
      expect(filterReq.request.params.get('type')).toBe('EMAIL_SENDING');
      filterReq.flush({ message: 'ok', code: 200, data: [] });
    }));
  });

  describe('AC12: Stats API errors - jobs still render', () => {
    it('shows jobs even when stats endpoints fail', () => {
      fixture.detectChanges();

      // Stats fail
      const summaryReq = httpMock.expectOne(r => r.url === `${dashboardUrl}/jobs/summary`);
      summaryReq.error(new ProgressEvent('error'));

      const metricsReq = httpMock.expectOne(r => r.url === `${dashboardUrl}/queue-metrics`);
      metricsReq.error(new ProgressEvent('error'));

      // Jobs succeed
      const jobsReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs`);
      jobsReq.flush({ message: 'ok', code: 200, data: mockJobs });

      fixture.detectChanges();

      expect(component.statsLoading).toBeFalse();
      expect(component.jobsLoading).toBeFalse();
      expect(component.jobs.length).toBe(3);

      const el = fixture.nativeElement;
      const rows = el.querySelectorAll('tbody tr');
      expect(rows.length).toBe(3);
    });
  });

  describe('AC13: Jobs API error - stats still render', () => {
    it('shows stats even when jobs endpoint fails', () => {
      fixture.detectChanges();

      // Stats succeed
      const summaryReq = httpMock.expectOne(r => r.url === `${dashboardUrl}/jobs/summary`);
      summaryReq.flush({
        message: 'ok', code: 200,
        data: { TOTAL: 50, PENDING: 10, PROCESSING: 5, COMPLETED: 25, FAILED: 5, DEAD: 3, CANCELED: 2 }
      });

      const metricsReq = httpMock.expectOne(r => r.url === `${dashboardUrl}/queue-metrics`);
      metricsReq.flush({
        message: 'ok', code: 200,
        data: {
          queues: {
            HIGH_PRIORITY_QUEUE_LENGTH: 3,
            MEDIUM_PRIORITY_QUEUE_LENGTH: 2,
            LOW_PRIORITY_QUEUE_LENGTH: 1,
            DELAY_QUEUE_LENGTH: 0,
            RETRY_QUEUE_LENGTH: 1,
            DEAD_LETTER_QUEUE_LENGTH: 0
          }
        }
      });

      // Jobs fail
      const jobsReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs`);
      jobsReq.error(new ProgressEvent('error'));

      fixture.detectChanges();

      expect(component.statsLoading).toBeFalse();
      expect(component.jobsLoading).toBeFalse();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('50');
      expect(el.textContent).toContain('Pending');
      expect(el.textContent).toContain('Queue Metrics');
    });
  });

  describe('Create Job modal integration', () => {
    it('opens modal when Create Job button is clicked', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      expect(component.showCreateModal).toBeFalse();

      const el = fixture.nativeElement;
      const createBtn = Array.from(el.querySelectorAll('button')).find(
        (b: any) => b.textContent.includes('Create Job')
      ) as any;
      createBtn.click();

      expect(component.showCreateModal).toBeTrue();
    });

    it('closes modal when onJobCreated is called', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      component.showCreateModal = true;
      component.onJobCreated();

      expect(component.showCreateModal).toBeFalse();

      const reloadReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs`);
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });

    it('refreshes job list when onJobCreated is called', () => {
      fixture.detectChanges();
      flushInitialRequests();
      fixture.detectChanges();

      component.onJobCreated();

      const reloadReq = httpMock.expectOne(r => r.url === `${jobsUrl}/jobs`);
      expect(reloadReq).toBeTruthy();
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });
  });
});
