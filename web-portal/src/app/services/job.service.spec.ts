import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { JobService } from './job.service';
import { environment } from '../../environments/environment';

describe('JobService', () => {
  let service: JobService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/v1`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [JobService]
    });
    service = TestBed.inject(JobService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('create', () => {
    it('POSTs to /v1/job/create with job request', () => {
      const request = {
        priority: 'HIGH' as const,
        type: 'EMAIL_SENDING' as const,
        payload: 'test payload'
      };

      service.create(request).subscribe(res => {
        expect(res.data.id).toBe('job-123');
      });

      const req = httpMock.expectOne(`${baseUrl}/job/create`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush({ message: 'ok', code: 200, data: { id: 'job-123' } });
    });
  });

  describe('list', () => {
    it('GETs /v1/jobs with page and size params', () => {
      service.list(1, 10).subscribe(res => {
        expect(res.data.length).toBe(1);
      });

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('1');
      expect(req.request.params.get('size')).toBe('10');
      req.flush({ message: 'ok', code: 200, data: [{ id: 'j1' }] });
    });

    it('uses default params page=0, size=20', () => {
      service.list().subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs`);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush({ message: 'ok', code: 200, data: [] });
    });
  });

  describe('get', () => {
    it('GETs /v1/jobs/:id', () => {
      service.get('job-abc').subscribe(res => {
        expect(res.data.id).toBe('job-abc');
      });

      const req = httpMock.expectOne(`${baseUrl}/jobs/job-abc`);
      expect(req.request.method).toBe('GET');
      req.flush({ message: 'ok', code: 200, data: { id: 'job-abc' } });
    });
  });

  describe('filter', () => {
    it('GETs /v1/jobs/filter with status param', () => {
      service.filter('PENDING').subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs/filter`);
      expect(req.request.params.get('status')).toBe('PENDING');
      expect(req.request.params.has('type')).toBeFalse();
      req.flush({ message: 'ok', code: 200, data: [] });
    });

    it('GETs /v1/jobs/filter with type param', () => {
      service.filter(undefined, 'EMAIL_SENDING').subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs/filter`);
      expect(req.request.params.get('type')).toBe('EMAIL_SENDING');
      expect(req.request.params.has('status')).toBeFalse();
      req.flush({ message: 'ok', code: 200, data: [] });
    });

    it('GETs /v1/jobs/filter with both status and type', () => {
      service.filter('DEAD', 'PAYMENT_SENDING', 2, 5).subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs/filter`);
      expect(req.request.params.get('status')).toBe('DEAD');
      expect(req.request.params.get('type')).toBe('PAYMENT_SENDING');
      expect(req.request.params.get('page')).toBe('2');
      expect(req.request.params.get('size')).toBe('5');
      req.flush({ message: 'ok', code: 200, data: [] });
    });

    it('omits undefined status and type params', () => {
      service.filter().subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/jobs/filter`);
      expect(req.request.params.has('status')).toBeFalse();
      expect(req.request.params.has('type')).toBeFalse();
      req.flush({ message: 'ok', code: 200, data: [] });
    });
  });

  describe('cancel', () => {
    it('POSTs to /v1/jobs/:id/cancel', () => {
      service.cancel('job-42').subscribe(res => {
        expect(res.message).toBe('cancelled');
      });

      const req = httpMock.expectOne(`${baseUrl}/jobs/job-42/cancel`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({ message: 'cancelled', code: 200, data: null });
    });
  });

  describe('revive', () => {
    it('POSTs to /v1/jobs/:id/revive', () => {
      service.revive('job-99').subscribe(res => {
        expect(res.data).toBe('revived');
      });

      const req = httpMock.expectOne(`${baseUrl}/jobs/job-99/revive`);
      expect(req.request.method).toBe('POST');
      req.flush({ message: 'ok', code: 200, data: 'revived' });
    });
  });

  describe('reviveAll', () => {
    it('POSTs to /v1/jobs/revive', () => {
      service.reviveAll().subscribe(res => {
        expect(res.data.length).toBe(2);
      });

      const req = httpMock.expectOne(`${baseUrl}/jobs/revive`);
      expect(req.request.method).toBe('POST');
      req.flush({ message: 'ok', code: 200, data: ['j1', 'j2'] });
    });
  });
});
