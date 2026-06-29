import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AlertComponent } from './alert.component';
import { AlertService } from '../../services/alert.service';

describe('AlertComponent', () => {
  let component: AlertComponent;
  let fixture: ComponentFixture<AlertComponent>;
  let alertService: AlertService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertComponent],
      providers: [
        AlertService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AlertComponent);
    component = fixture.componentInstance;
    alertService = TestBed.inject(AlertService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('AC9/AC13: error messages shown on API failures', () => {
    it('shows alert message when AlertService emits', () => {
      alertService.show('Test alert', 'danger');
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const alertEl = el.querySelector('.alert');
      expect(alertEl).toBeTruthy();
      expect(alertEl.textContent).toContain('Test alert');
    });

    it('applies correct alert type class', () => {
      alertService.show('Warning', 'warning');
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const alertEl = el.querySelector('.alert-warning');
      expect(alertEl).toBeTruthy();
    });

    it('hides when no message', () => {
      component.message = '';
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const alertEl = el.querySelector('.alert');
      expect(alertEl).toBeFalsy();
    });
  });

  describe('dismiss', () => {
    it('clears the alert when dismiss is called', () => {
      alertService.show('Dismissible', 'info');
      fixture.detectChanges();

      component.dismiss();
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const alertEl = el.querySelector('.alert');
      expect(alertEl).toBeFalsy();
    });
  });

  describe('accessibility', () => {
    it('has role="alert" on the alert element', () => {
      alertService.show('Accessible alert', 'success');
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const alertEl = el.querySelector('[role="alert"]');
      expect(alertEl).toBeTruthy();
    });

    it('has aria-label on the close button', () => {
      alertService.show('Closeable', 'info');
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const closeBtn = el.querySelector('.btn-close');
      expect(closeBtn).toBeTruthy();
      expect(closeBtn.getAttribute('aria-label')).toBe('Close');
    });
  });
});
