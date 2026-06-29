import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { JobDetailComponent } from './job-detail.component';
import { JobService } from '../../services/job.service';
import { AlertService } from '../../services/alert.service';
import { environment } from '../../../environments/environment';

describe('JobDetailComponent', () => {
  let component: JobDetailComponent;
  let fixture: ComponentFixture<JobDetailComponent>;
  let jobService: JobService;
  let alertService: AlertService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/v1`;

  const mockJob = {
    id: 'job-001-abc-def-123',
    priority: 'HIGH',
    type: 'EMAIL_SENDING',
    status: 'PENDING',
    dependents: ['job-004-jkl'],
    dependencies: [],
    attemptCount: 1,
    maxAttemptCount: 5,
    startedAt: '2025-01-01T10:00:00Z',
    result: '{"to":"test@example.com"}'
  };

  function setup(routeId: string = 'job-001-abc-def-123') {
    TestBed.configureTestingModule({
      imports: [JobDetailComponent, RouterTestingModule, HttpClientTestingModule],
      providers: [
        JobService,
        AlertService,
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => key === 'id' ? routeId : null
              }
            }
          }
        }
      ]
    });

    fixture = TestBed.createComponent(JobDetailComponent);
    component = fixture.componentInstance;
    jobService = TestBed.inject(JobService);
    alertService = TestBed.inject(AlertService);
    httpMock = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    setup();
    expect(component).toBeTruthy();
  });

  describe('AC4: Job detail shows all info', () => {
    it('displays job ID', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('job-001-abc-def-123');
    });

    it('displays job status', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('PENDING');
    });

    it('displays job priority', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('HIGH');
    });

    it('displays job type', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('Email Sending');
    });

    it('displays attempt count', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('1 / 5');
    });

    it('displays payload/result', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('test@example.com');
    });

    it('displays dependencies section', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('Dependencies');
    });

    it('shows "None" when no dependencies', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const depSection = el.querySelector('.col-md-4');
      expect(depSection.textContent).toContain('None');
    });

    it('displays dependents section with links', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('Dependents');
      const depLinks = el.querySelectorAll('.col-md-4 a');
      expect(depLinks.length).toBe(1);
    });

    it('displays timestamps', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('Started At');
    });
  });

  describe('AC6: Cancel button on detail page', () => {
    it('shows cancel button for PENDING jobs', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob }); // PENDING
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtn = el.querySelector('.btn-outline-danger');
      expect(cancelBtn).toBeTruthy();
      expect(cancelBtn.textContent).toContain('Cancel');
    });

    it('shows cancel button for FAILED jobs', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: { ...mockJob, status: 'FAILED' } });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtn = el.querySelector('.btn-outline-danger');
      expect(cancelBtn).toBeTruthy();
    });

    it('calls cancel and updates status', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      component.cancel();

      const cancelReq = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123/cancel`);
      cancelReq.flush({ message: 'ok', code: 200, data: null });

      expect(component.job?.status).toBe('CANCELED');
    });
  });

  describe('AC10: Cannot cancel CANCELED/COMPLETED jobs', () => {
    it('hides cancel button for CANCELED jobs', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: { ...mockJob, status: 'CANCELED' } });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtn = el.querySelector('.btn-outline-danger');
      expect(cancelBtn).toBeFalsy();
    });

    it('hides cancel button for COMPLETED jobs', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: { ...mockJob, status: 'COMPLETED' } });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const cancelBtn = el.querySelector('.btn-outline-danger');
      expect(cancelBtn).toBeFalsy();
    });
  });

  describe('AC7: Revive button on detail page', () => {
    it('shows revive button for DEAD jobs', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: { ...mockJob, status: 'DEAD' } });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const reviveBtn = el.querySelector('.btn-outline-success');
      expect(reviveBtn).toBeTruthy();
      expect(reviveBtn.textContent).toContain('Revive');
    });

    it('calls revive and updates status', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: { ...mockJob, status: 'DEAD' } });
      fixture.detectChanges();

      component.revive();

      const reviveReq = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123/revive`);
      reviveReq.flush({ message: 'ok', code: 200, data: 'revived' });

      expect(component.job?.status).toBe('PENDING');
    });
  });

  describe('AC11: Cannot revive non-DEAD jobs', () => {
    it('hides revive button for PENDING jobs', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob }); // PENDING
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const reviveBtn = el.querySelector('.btn-outline-success');
      expect(reviveBtn).toBeFalsy();
    });
  });

  describe('job not found', () => {
    it('shows not found message when API returns error', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.error(new ProgressEvent('error'), { status: 404 });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('Job not found');
    });
  });

  describe('back link', () => {
    it('has a link back to job list', () => {
      setup();
      fixture.detectChanges();
      const req = httpMock.expectOne(`${baseUrl}/jobs/job-001-abc-def-123`);
      req.flush({ message: 'ok', code: 200, data: mockJob });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const backLink = el.querySelector('a[href="/jobs"]');
      expect(backLink).toBeTruthy();
    });
  });
});
