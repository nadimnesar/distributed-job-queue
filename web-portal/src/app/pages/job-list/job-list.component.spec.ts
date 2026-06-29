import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { JobListComponent } from './job-list.component';
import { JobCreateModalComponent } from '../../components/job-create-modal/job-create-modal.component';
import { JobService } from '../../services/job.service';
import { AlertService } from '../../services/alert.service';
import { environment } from '../../../environments/environment';

describe('JobListComponent', () => {
  let component: JobListComponent;
  let fixture: ComponentFixture<JobListComponent>;
  let jobService: JobService;
  let alertService: AlertService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/v1`;

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

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [JobListComponent, JobCreateModalComponent, RouterTestingModule, ReactiveFormsModule],
      providers: [
        JobService,
        AlertService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    fixture = TestBed.createComponent(JobListComponent);
    component = fixture.componentInstance;
    jobService = TestBed.inject(JobService);
    alertService = TestBed.inject(AlertService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('AC2: Job list shows paginated table', () => {
    it('renders a table with column headers', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: mockJobs });
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
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: mockJobs });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const rows = el.querySelectorAll('tbody tr');
      expect(rows.length).toBe(3);
    });

    it('displays truncated job ID', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: mockJobs });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('job-001');
    });

    it('displays job type formatted', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: mockJobs });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('Email Sending');
      expect(el.textContent).toContain('Payment Sending');
    });

    it('displays pagination controls', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: mockJobs });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('Page 1');
      expect(el.textContent).toContain('Previous');
      expect(el.textContent).toContain('Next');
    });
  });

  describe('AC3: Job list supports filtering', () => {
    it('renders filter form with status and type selects', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const selects = el.querySelectorAll('select');
      expect(selects.length).toBe(2);
    });

    it('populates status select with all job statuses', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
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
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const typeSelect = el.querySelectorAll('select')[1];
      const options = Array.from(typeSelect.querySelectorAll('option')).map((o: any) => o.value);
      expect(options).toContain('EMAIL_SENDING');
      expect(options).toContain('PAYMENT_SENDING');
    });

    it('calls filter endpoint when status is selected', fakeAsync(() => {
      fixture.detectChanges();
      const listReq = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      listReq.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      component.filterForm.patchValue({ status: 'PENDING', type: '' });
      tick(400); // debounce time is 300ms
      fixture.detectChanges();

      const filterReq = httpMock.expectOne(r => r.url === `${baseUrl}/jobs/filter`);
      expect(filterReq.request.params.get('status')).toBe('PENDING');
      filterReq.flush({ message: 'ok', code: 200, data: [] });
    }));

    it('calls filter endpoint when type is selected', fakeAsync(() => {
      fixture.detectChanges();
      const listReq = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      listReq.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      component.filterForm.patchValue({ status: '', type: 'EMAIL_SENDING' });
      tick(400);
      fixture.detectChanges();

      const filterReq = httpMock.expectOne(r => r.url === `${baseUrl}/jobs/filter`);
      expect(filterReq.request.params.get('type')).toBe('EMAIL_SENDING');
      filterReq.flush({ message: 'ok', code: 200, data: [] });
    }));

    it('clearFilters resets both form controls', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      component.filterForm.patchValue({ status: 'DEAD', type: 'PAYMENT_SENDING' });
      component.clearFilters();

      expect(component.filterForm.value.status).toBe('');
      expect(component.filterForm.value.type).toBe('');
    });
  });

  describe('AC6: Cancel button works for PENDING/PROCESSING jobs', () => {
    it('cancel button is enabled for PENDING jobs', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [mockJobs[0]] }); // PENDING
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtns = el.querySelectorAll('table .btn-outline-danger');
      expect(cancelBtns.length).toBe(1);
      expect(cancelBtns[0].disabled).toBeFalse();
    });

    it('cancel button is enabled for FAILED jobs', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({
        message: 'ok', code: 200,
        data: [{ ...mockJobs[0], status: 'FAILED' }]
      });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtns = el.querySelectorAll('table .btn-outline-danger');
      expect(cancelBtns.length).toBe(1);
      expect(cancelBtns[0].disabled).toBeFalse();
    });

    it('calls cancel endpoint when cancel is clicked', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [mockJobs[0]] });
      fixture.detectChanges();

      component.cancelJob(mockJobs[0] as any);

      const cancelReq = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc/cancel`);
      expect(cancelReq.request.method).toBe('POST');
      cancelReq.flush({ message: 'ok', code: 200, data: null });

      // After cancel, list is reloaded
      const reloadReq = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });
  });

  describe('AC10: Cannot cancel CANCELED/COMPLETED jobs', () => {
    it('cancel button is disabled for CANCELED jobs', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({
        message: 'ok', code: 200,
        data: [{ ...mockJobs[0], status: 'CANCELED' }]
      });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtns = el.querySelectorAll('table .btn-outline-danger');
      expect(cancelBtns[0].disabled).toBeTrue();
    });

    it('cancel button is disabled for COMPLETED jobs', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({
        message: 'ok', code: 200,
        data: [{ ...mockJobs[0], status: 'COMPLETED' }]
      });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtns = el.querySelectorAll('table .btn-outline-danger');
      expect(cancelBtns[0].disabled).toBeTrue();
    });

    it('cancel button is disabled for DEAD jobs', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({
        message: 'ok', code: 200,
        data: [{ ...mockJobs[0], status: 'DEAD' }]
      });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtns = el.querySelectorAll('table .btn-outline-danger');
      expect(cancelBtns[0].disabled).toBeTrue();
    });
  });

  describe('AC7: Revive button works for DEAD jobs', () => {
    it('revive button is enabled for DEAD jobs', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [mockJobs[2]] }); // DEAD
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const reviveBtns = el.querySelectorAll('table .btn-outline-success');
      expect(reviveBtns.length).toBe(1);
      expect(reviveBtns[0].disabled).toBeFalse();
    });

    it('calls revive endpoint when revive is clicked', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [mockJobs[2]] });
      fixture.detectChanges();

      component.reviveJob(mockJobs[2] as any);

      const reviveReq = httpMock.expectOne(`${baseUrl}/jobs/job-003-ghi/revive`);
      expect(reviveReq.request.method).toBe('POST');
      reviveReq.flush({ message: 'ok', code: 200, data: 'revived' });

      const reloadReq = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });
  });

  describe('AC11: Cannot revive non-DEAD jobs', () => {
    it('revive button is disabled for PENDING jobs', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [mockJobs[0]] }); // PENDING
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const reviveBtns = el.querySelectorAll('table .btn-outline-success');
      expect(reviveBtns[0].disabled).toBeTrue();
    });

    it('revive button is disabled for COMPLETED jobs', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [mockJobs[1]] }); // COMPLETED
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const reviveBtns = el.querySelectorAll('table .btn-outline-success');
      expect(reviveBtns[0].disabled).toBeTrue();
    });
  });

  describe('AC8: Revive All Dead Jobs button', () => {
    it('has a Revive All Dead button', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const btns = Array.from(el.querySelectorAll('button')).filter(
        (b: any) => b.textContent.includes('Revive All')
      );
      expect(btns.length).toBe(1);
    });

    it('calls reviveAll endpoint when clicked', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      component.reviveAll();

      const reviveReq = httpMock.expectOne(`${baseUrl}/jobs/revive`);
      expect(reviveReq.request.method).toBe('POST');
      reviveReq.flush({ message: 'ok', code: 200, data: ['j1', 'j2'] });

      const reloadReq = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });
  });

  describe('AC12: Empty job list shows "No jobs found"', () => {
    it('displays no jobs message when list is empty', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('No jobs found');
    });

    it('does not show table when list is empty', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const table = el.querySelector('table');
      expect(table).toBeFalsy();
    });
  });

  describe('pagination', () => {
    it('disables Previous on first page', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: mockJobs });
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
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: mockJobs });
      fixture.detectChanges();

      component.goPage(1);

      expect(component.page).toBe(1);
      const reloadReq = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });
  });

  describe('create job modal integration', () => {
    it('has a Create Job button', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const createBtn = Array.from(el.querySelectorAll('button')).find(
        (b: any) => b.textContent.includes('Create Job')
      ) as any;
      expect(createBtn).toBeTruthy();
    });

    it('does not have a routerLink to /jobs/create', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const createLink = el.querySelector('a[href="/jobs/create"]');
      expect(createLink).toBeFalsy();
    });

    it('opens modal when Create Job button is clicked', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
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
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      component.showCreateModal = true;
      component.onJobCreated();

      expect(component.showCreateModal).toBeFalse();

      const reloadReq = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });

    it('refreshes job list when onJobCreated is called', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      req.flush({ message: 'ok', code: 200, data: [] });
      fixture.detectChanges();

      component.onJobCreated();

      const reloadReq = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      reloadReq.flush({ message: 'ok', code: 200, data: [] });
    });
  });
});
