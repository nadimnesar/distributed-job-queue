import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DashboardService } from './dashboard.service';
import { environment } from '../../environments/environment';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/v1/dashboard`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DashboardService]
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getJobSummary', () => {
    it('GETs /v1/dashboard/jobs/summary', () => {
      service.getJobSummary().subscribe(res => {
        expect(res.data.TOTAL).toBe(100);
        expect(res.data.PENDING).toBe(10);
        expect(res.data.PROCESSING).toBe(5);
        expect(res.data.COMPLETED).toBe(70);
        expect(res.data.FAILED).toBe(10);
        expect(res.data.DEAD).toBe(3);
        expect(res.data.CANCELED).toBe(2);
      });

      const req = httpMock.expectOne(`${baseUrl}/jobs/summary`);
      expect(req.request.method).toBe('GET');
      req.flush({
        message: 'ok', code: 200,
        data: { TOTAL: 100, PENDING: 10, PROCESSING: 5, COMPLETED: 70, FAILED: 10, DEAD: 3, CANCELED: 2 }
      });
    });
  });

  describe('getQueueMetrics', () => {
    it('GETs /v1/dashboard/queue-metrics', () => {
      service.getQueueMetrics().subscribe(res => {
        expect(res.data.queues.HIGH_PRIORITY_QUEUE_LENGTH).toBe(5);
        expect(res.data.queues.MEDIUM_PRIORITY_QUEUE_LENGTH).toBe(3);
        expect(res.data.queues.LOW_PRIORITY_QUEUE_LENGTH).toBe(2);
        expect(res.data.queues.RETRY_QUEUE_LENGTH).toBe(1);
        expect(res.data.queues.DEAD_LETTER_QUEUE_LENGTH).toBe(0);
      });

      const req = httpMock.expectOne(`${baseUrl}/queue-metrics`);
      expect(req.request.method).toBe('GET');
      req.flush({
        message: 'ok', code: 200,
        data: {
          queues: {
            HIGH_PRIORITY_QUEUE_LENGTH: 5,
            MEDIUM_PRIORITY_QUEUE_LENGTH: 3,
            LOW_PRIORITY_QUEUE_LENGTH: 2,
            RETRY_QUEUE_LENGTH: 1,
            DEAD_LETTER_QUEUE_LENGTH: 0
          }
        }
      });
    });
  });
});
