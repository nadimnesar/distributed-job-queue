import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { errorInterceptor } from './error.interceptor';
import { AlertService } from '../services/alert.service';

describe('errorInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let alertService: AlertService;
  let alertMessages: string[];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AlertService,
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting()
      ]
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
    alertService = TestBed.inject(AlertService);
    alertMessages = [];

    alertService.alert$.subscribe(alert => {
      if (alert.message) alertMessages.push(alert.message);
    });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(errorInterceptor).toBeTruthy();
  });

  describe('status 0 (network error)', () => {
    it('shows service unavailable message', () => {
      httpClient.get('/api/test').subscribe({
        error: () => {}
      });

      const req = httpMock.expectOne('/api/test');
      req.error(new ProgressEvent('error'), { status: 0 });

      expect(alertMessages.length).toBe(1);
      expect(alertMessages[0]).toContain('Service unavailable');
      expect(alertMessages[0]).toContain('connection');
    });
  });

  describe('server error with error.message', () => {
    it('shows the server error message', () => {
      httpClient.get('/api/test').subscribe({
        error: () => {}
      });

      const req = httpMock.expectOne('/api/test');
      req.flush(
        { message: 'Job not found' },
        { status: 404, statusText: 'Not Found' }
      );

      expect(alertMessages.length).toBe(1);
      expect(alertMessages[0]).toBe('Job not found');
    });
  });

  describe('server error without error.message', () => {
    it('shows the HTTP error message', () => {
      httpClient.get('/api/test').subscribe({
        error: () => {}
      });

      const req = httpMock.expectOne('/api/test');
      req.flush('', { status: 500, statusText: 'Internal Server Error' });

      expect(alertMessages.length).toBe(1);
      expect(alertMessages[0]).toContain('500');
    });
  });

  describe('unexpected error with no details', () => {
    it('shows generic error message', () => {
      httpClient.get('/api/test').subscribe({
        error: () => {}
      });

      const req = httpMock.expectOne('/api/test');
      req.error(new ProgressEvent('error'), { status: 500 });

      expect(alertMessages.length).toBe(1);
      expect(alertMessages[0]).toBe('An unexpected error occurred.');
    });
  });

  describe('re-throws error', () => {
    it('propagates the error to the caller', () => {
      let caughtError: any;
      httpClient.get('/api/test').subscribe({
        error: (err) => { caughtError = err; }
      });

      const req = httpMock.expectOne('/api/test');
      req.flush('error', { status: 500, statusText: 'Server Error' });

      expect(caughtError).toBeTruthy();
      expect(caughtError.status).toBe(500);
    });
  });
});
