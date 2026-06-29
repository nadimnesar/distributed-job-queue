import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DashboardComponent } from './dashboard.component';
import { DashboardService } from '../../services/dashboard.service';
import { environment } from '../../../environments/environment';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let dashboardService: DashboardService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/v1/dashboard`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        DashboardService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    dashboardService = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('AC1: Dashboard shows job summary and queue metrics', () => {
    it('renders summary cards after data loads', () => {
      fixture.detectChanges();

      // Respond to both API calls
      const summaryReq = httpMock.expectOne(`${baseUrl}/jobs/summary`);
      summaryReq.flush({
        message: 'ok', code: 200,
        data: { TOTAL: 50, PENDING: 10, PROCESSING: 5, COMPLETED: 25, FAILED: 5, DEAD: 3, CANCELED: 2 }
      });

      const metricsReq = httpMock.expectOne(`${baseUrl}/queue-metrics`);
      metricsReq.flush({
        message: 'ok', code: 200,
        data: {
          queues: {
            HIGH_PRIORITY_QUEUE_LENGTH: 3,
            MEDIUM_PRIORITY_QUEUE_LENGTH: 2,
            LOW_PRIORITY_QUEUE_LENGTH: 1,
            RETRY_QUEUE_LENGTH: 1,
            DEAD_LETTER_QUEUE_LENGTH: 0
          }
        }
      });

      fixture.detectChanges();

      const el = fixture.nativeElement;
      // Check summary cards rendered
      const cards = el.querySelectorAll('.card');
      expect(cards.length).toBeGreaterThanOrEqual(7); // 7 summary + 5 queue

      // Check total count is displayed
      expect(el.textContent).toContain('50');
      expect(el.textContent).toContain('Pending');
      expect(el.textContent).toContain('Processing');
    });

    it('renders queue metrics section', () => {
      fixture.detectChanges();

      const summaryReq = httpMock.expectOne(`${baseUrl}/jobs/summary`);
      summaryReq.flush({
        message: 'ok', code: 200,
        data: { TOTAL: 0, PENDING: 0, PROCESSING: 0, COMPLETED: 0, FAILED: 0, DEAD: 0, CANCELED: 0 }
      });

      const metricsReq = httpMock.expectOne(`${baseUrl}/queue-metrics`);
      metricsReq.flush({
        message: 'ok', code: 200,
        data: {
          queues: {
            HIGH_PRIORITY_QUEUE_LENGTH: 1,
            MEDIUM_PRIORITY_QUEUE_LENGTH: 0,
            LOW_PRIORITY_QUEUE_LENGTH: 0,
            RETRY_QUEUE_LENGTH: 0,
            DEAD_LETTER_QUEUE_LENGTH: 0
          }
        }
      });

      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.textContent).toContain('Queue Metrics');
      expect(el.textContent).toContain('High');
      expect(el.textContent).toContain('Dead Letter');
    });
  });

  describe('loading state', () => {
    it('shows spinner while loading', () => {
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const spinner = el.querySelector('.spinner-border');
      expect(spinner).toBeTruthy();
    });

    it('hides spinner after data loads', () => {
      fixture.detectChanges();

      const summaryReq = httpMock.expectOne(`${baseUrl}/jobs/summary`);
      summaryReq.flush({ message: 'ok', code: 200, data: { TOTAL: 0, PENDING: 0, PROCESSING: 0, COMPLETED: 0, FAILED: 0, DEAD: 0, CANCELED: 0 } });

      const metricsReq = httpMock.expectOne(`${baseUrl}/queue-metrics`);
      metricsReq.flush({ message: 'ok', code: 200, data: { queues: { HIGH_PRIORITY_QUEUE_LENGTH: 0, MEDIUM_PRIORITY_QUEUE_LENGTH: 0, LOW_PRIORITY_QUEUE_LENGTH: 0, RETRY_QUEUE_LENGTH: 0, DEAD_LETTER_QUEUE_LENGTH: 0 } } });

      fixture.detectChanges();

      const el = fixture.nativeElement;
      const spinner = el.querySelector('.spinner-border');
      expect(spinner).toBeFalsy();
    });
  });

  describe('refresh button', () => {
    it('has a refresh button', () => {
      fixture.detectChanges();

      const summaryReq = httpMock.expectOne(`${baseUrl}/jobs/summary`);
      summaryReq.flush({ message: 'ok', code: 200, data: { TOTAL: 0, PENDING: 0, PROCESSING: 0, COMPLETED: 0, FAILED: 0, DEAD: 0, CANCELED: 0 } });

      const metricsReq = httpMock.expectOne(`${baseUrl}/queue-metrics`);
      metricsReq.flush({ message: 'ok', code: 200, data: { queues: { HIGH_PRIORITY_QUEUE_LENGTH: 0, MEDIUM_PRIORITY_QUEUE_LENGTH: 0, LOW_PRIORITY_QUEUE_LENGTH: 0, RETRY_QUEUE_LENGTH: 0, DEAD_LETTER_QUEUE_LENGTH: 0 } } });

      fixture.detectChanges();

      const el = fixture.nativeElement;
      const btn = el.querySelector('button');
      expect(btn).toBeTruthy();
      expect(btn.textContent).toContain('Refresh');
    });
  });

  describe('error handling', () => {
    it('stops loading even if API fails', () => {
      fixture.detectChanges();

      const summaryReq = httpMock.expectOne(`${baseUrl}/jobs/summary`);
      summaryReq.error(new ProgressEvent('error'));

      const metricsReq = httpMock.expectOne(`${baseUrl}/queue-metrics`);
      metricsReq.error(new ProgressEvent('error'));

      fixture.detectChanges();

      expect(component.loading).toBeFalse();
    });
  });
});
