import { TestBed } from '@angular/core/testing';
import { AlertService } from './alert.service';

describe('AlertService', () => {
  let service: AlertService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AlertService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('show', () => {
    it('emits alert with given message and type', () => {
      const alerts: any[] = [];
      service.alert$.subscribe(a => alerts.push(a));

      service.show('Test message', 'success');
      expect(alerts.length).toBe(1);
      expect(alerts[0].message).toBe('Test message');
      expect(alerts[0].type).toBe('success');
    });

    it('defaults to info type when not specified', () => {
      const alerts: any[] = [];
      service.alert$.subscribe(a => alerts.push(a));

      service.show('Info message');
      expect(alerts[0].type).toBe('info');
    });

    it('auto-clears after specified duration', (done) => {
      const alerts: any[] = [];
      service.alert$.subscribe(a => alerts.push(a));

      service.show('Temporary', 'warning', 100);
      expect(alerts.length).toBe(1);

      setTimeout(() => {
        expect(alerts.length).toBe(2);
        expect(alerts[1].message).toBe('');
        done();
      }, 150);
    });

    it('does not auto-clear when duration is 0', (done) => {
      const alerts: any[] = [];
      service.alert$.subscribe(a => alerts.push(a));

      service.show('Persistent', 'info', 0);
      expect(alerts.length).toBe(1);

      setTimeout(() => {
        expect(alerts.length).toBe(1);
        done();
      }, 100);
    });
  });

  describe('convenience methods', () => {
    it('success() emits success type alert', () => {
      const alerts: any[] = [];
      service.alert$.subscribe(a => alerts.push(a));

      service.success('Done!');
      expect(alerts[0].type).toBe('success');
      expect(alerts[0].message).toBe('Done!');
    });

    it('error() emits danger type alert with longer duration', () => {
      const alerts: any[] = [];
      service.alert$.subscribe(a => alerts.push(a));

      service.error('Oops');
      expect(alerts[0].type).toBe('danger');
      expect(alerts[0].message).toBe('Oops');
    });

    it('warning() emits warning type alert', () => {
      const alerts: any[] = [];
      service.alert$.subscribe(a => alerts.push(a));

      service.warning('Careful');
      expect(alerts[0].type).toBe('warning');
      expect(alerts[0].message).toBe('Careful');
    });
  });

  describe('clear', () => {
    it('emits empty message to dismiss alert', () => {
      const alerts: any[] = [];
      service.alert$.subscribe(a => alerts.push(a));

      service.success('Active');
      service.clear();
      expect(alerts.length).toBe(2);
      expect(alerts[1].message).toBe('');
    });
  });

  describe('replacement behavior', () => {
    it('replaces previous alert when show is called again', () => {
      const alerts: any[] = [];
      service.alert$.subscribe(a => alerts.push(a));

      service.show('First', 'success');
      service.show('Second', 'warning');
      expect(alerts.length).toBe(2);
      expect(alerts[0].message).toBe('First');
      expect(alerts[1].message).toBe('Second');
    });
  });
});
