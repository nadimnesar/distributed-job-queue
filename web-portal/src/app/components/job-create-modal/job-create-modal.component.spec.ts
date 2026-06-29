import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';
import { JobCreateModalComponent } from './job-create-modal.component';
import { AlertService } from '../../services/alert.service';
import { JOB_PRIORITIES, JOB_TYPES } from '../../models/job.model';

describe('JobCreateModalComponent', () => {
  let component: JobCreateModalComponent;
  let fixture: ComponentFixture<JobCreateModalComponent>;
  let httpMock: HttpTestingController;
  let alertService: AlertService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobCreateModalComponent, ReactiveFormsModule],
      providers: [
        AlertService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(JobCreateModalComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    alertService = TestBed.inject(AlertService);

    // Set isOpen to true so the modal renders
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('AC1: Modal renders when isOpen is true', () => {
    it('renders the modal dialog when isOpen is true', () => {
      const el = fixture.nativeElement;
      expect(el.querySelector('.modal')).toBeTruthy();
      expect(el.querySelector('.modal-dialog')).toBeTruthy();
      expect(el.querySelector('.modal-content')).toBeTruthy();
    });

    it('does not render modal when isOpen is false', () => {
      fixture.componentRef.setInput('isOpen', false);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      expect(el.querySelector('.modal')).toBeFalsy();
    });

    it('has modal-header with title', () => {
      const el = fixture.nativeElement;
      const title = el.querySelector('.modal-title');
      expect(title).toBeTruthy();
      expect(title.textContent).toContain('Create Job');
    });

    it('has close button with aria-label', () => {
      const el = fixture.nativeElement;
      const closeBtn = el.querySelector('.btn-close');
      expect(closeBtn).toBeTruthy();
      expect(closeBtn.getAttribute('aria-label')).toBe('Close');
    });

    it('has aria-modal="true" on the modal', () => {
      const el = fixture.nativeElement;
      const modal = el.querySelector('.modal');
      expect(modal.getAttribute('aria-modal')).toBe('true');
    });
  });

  describe('AC2: Form fields match existing JobCreateComponent', () => {
    it('has priority select with JOB_PRIORITIES options', () => {
      const el = fixture.nativeElement;
      const select = el.querySelector('#priority');
      expect(select).toBeTruthy();
      const options = select.querySelectorAll('option');
      // 1 empty + JOB_PRIORITIES.length
      expect(options.length).toBe(1 + JOB_PRIORITIES.length);
    });

    it('has type select with JOB_TYPES options', () => {
      const el = fixture.nativeElement;
      const select = el.querySelector('#type');
      expect(select).toBeTruthy();
      const options = select.querySelectorAll('option');
      expect(options.length).toBe(1 + JOB_TYPES.length);
    });

    it('has payload textarea', () => {
      const el = fixture.nativeElement;
      const textarea = el.querySelector('#payload');
      expect(textarea).toBeTruthy();
      expect(textarea.tagName).toBe('TEXTAREA');
    });

    it('has maxAttemptCount number input', () => {
      const el = fixture.nativeElement;
      const input = el.querySelector('#maxAttemptCount');
      expect(input).toBeTruthy();
      expect(input.type).toBe('number');
    });

    it('has dependenciesRaw textarea', () => {
      const el = fixture.nativeElement;
      const textarea = el.querySelector('#dependenciesRaw');
      expect(textarea).toBeTruthy();
      expect(textarea.tagName).toBe('TEXTAREA');
    });
  });

  describe('Form validation', () => {
    it('form is invalid when empty', () => {
      expect(component.form.valid).toBeFalse();
    });

    it('form is valid when all required fields are filled', () => {
      component.form.patchValue({
        priority: 'HIGH',
        type: 'EMAIL_SENDING',
        payload: 'test payload'
      });
      expect(component.form.valid).toBeTrue();
    });

    it('submit button is disabled when form is invalid', () => {
      const el = fixture.nativeElement;
      const submitBtn = el.querySelector('.modal-footer .btn-primary');
      expect(submitBtn.disabled).toBeTrue();
    });

    it('submit button is enabled when form is valid', () => {
      component.form.patchValue({
        priority: 'HIGH',
        type: 'EMAIL_SENDING',
        payload: 'test payload'
      });
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const submitBtn = el.querySelector('.modal-footer .btn-primary');
      expect(submitBtn.disabled).toBeFalse();
    });
  });

  describe('AC3: Modal closes on backdrop click and Escape', () => {
    it('emits close when backdrop is clicked', () => {
      spyOn(component.close, 'emit');

      const modal = fixture.nativeElement.querySelector('.modal');
      modal.click(new MouseEvent('click', { bubbles: true }));

      expect(component.close.emit).toHaveBeenCalled();
    });

    it('does not emit close when modal-dialog is clicked', () => {
      spyOn(component.close, 'emit');

      const dialog = fixture.nativeElement.querySelector('.modal-dialog');
      dialog.click(new MouseEvent('click', { bubbles: true }));

      expect(component.close.emit).not.toHaveBeenCalled();
    });

    it('emits close when cancel button is clicked', () => {
      spyOn(component.close, 'emit');

      const cancelBtn = fixture.nativeElement.querySelector('.modal-footer .btn-outline-secondary');
      cancelBtn.click();

      expect(component.close.emit).toHaveBeenCalled();
    });

    it('emits close when header close button is clicked', () => {
      spyOn(component.close, 'emit');

      const closeBtn = fixture.nativeElement.querySelector('.modal-header .btn-close');
      closeBtn.click();

      expect(component.close.emit).toHaveBeenCalled();
    });

    it('emits close when Escape key is pressed', () => {
      spyOn(component.close, 'emit');

      const event = new KeyboardEvent('keydown', { key: 'Escape', bubbles: true });
      document.dispatchEvent(event);

      expect(component.close.emit).toHaveBeenCalled();
    });

    it('does not emit close when Escape key is pressed while modal is closed', () => {
      fixture.componentRef.setInput('isOpen', false);
      fixture.detectChanges();
      spyOn(component.close, 'emit');

      const event = new KeyboardEvent('keydown', { key: 'Escape', bubbles: true });
      document.dispatchEvent(event);

      expect(component.close.emit).not.toHaveBeenCalled();
    });
  });

  describe('Submit behavior', () => {
    it('shows loading spinner during submission', () => {
      component.form.patchValue({
        priority: 'HIGH',
        type: 'EMAIL_SENDING',
        payload: 'test payload'
      });
      fixture.detectChanges();

      component.submitting.set(true);
      fixture.detectChanges();

      const el = fixture.nativeElement;
      const spinner = el.querySelector('.spinner-border');
      expect(spinner).toBeTruthy();
    });

    it('calls jobService.create() on valid submit', () => {
      spyOn(component['jobService'], 'create').and.returnValue({
        subscribe: (callbacks: any) => {
          callbacks.next({ data: { id: 'new-job-id' } });
        }
      } as any);

      component.form.patchValue({
        priority: 'HIGH',
        type: 'EMAIL_SENDING',
        payload: 'test payload'
      });

      component.submit();

      expect(component['jobService'].create).toHaveBeenCalledWith({
        priority: 'HIGH',
        type: 'EMAIL_SENDING',
        payload: 'test payload',
        dependencies: undefined,
        maxAttemptCount: undefined
      });
    });

    it('parses comma-separated dependencies', () => {
      spyOn(component['jobService'], 'create').and.returnValue({
        subscribe: (callbacks: any) => {
          callbacks.next({ data: { id: 'new-job-id' } });
        }
      } as any);

      component.form.patchValue({
        priority: 'HIGH',
        type: 'EMAIL_SENDING',
        payload: 'test payload',
        dependenciesRaw: 'uuid1, uuid2 ,uuid3'
      });

      component.submit();

      expect(component['jobService'].create).toHaveBeenCalledWith(
        jasmine.objectContaining({
          dependencies: ['uuid1', 'uuid2', 'uuid3']
        })
      );
    });

    it('emits jobCreated with job ID on successful creation', () => {
      spyOn(component.jobCreated, 'emit');
      spyOn(component.close, 'emit');

      spyOn(component['jobService'], 'create').and.returnValue({
        subscribe: (callbacks: any) => {
          callbacks.next({ data: { id: 'created-job-id' } });
        }
      } as any);

      component.form.patchValue({
        priority: 'HIGH',
        type: 'EMAIL_SENDING',
        payload: 'test payload'
      });

      component.submit();

      expect(component.jobCreated.emit).toHaveBeenCalledWith('created-job-id');
      expect(component.close.emit).toHaveBeenCalled();
    });

    it('resets form after successful creation', () => {
      spyOn(component['jobService'], 'create').and.returnValue({
        subscribe: (callbacks: any) => {
          callbacks.next({ data: { id: 'new-job-id' } });
        }
      } as any);

      component.form.patchValue({
        priority: 'HIGH',
        type: 'EMAIL_SENDING',
        payload: 'test payload'
      });

      component.submit();

      expect(component.form.pristine).toBeTrue();
    });

    it('shows success alert after creation', () => {
      spyOn(alertService, 'success');

      spyOn(component['jobService'], 'create').and.returnValue({
        subscribe: (callbacks: any) => {
          callbacks.next({ data: { id: 'new-job-id' } });
        }
      } as any);

      component.form.patchValue({
        priority: 'HIGH',
        type: 'EMAIL_SENDING',
        payload: 'test payload'
      });

      component.submit();

      expect(alertService.success).toHaveBeenCalledWith('Job created successfully');
    });
  });

  describe('Accessibility', () => {
    it('has associated labels for form inputs', () => {
      const el = fixture.nativeElement;
      const labels = el.querySelectorAll('label');
      labels.forEach((label: HTMLLabelElement) => {
        const forAttr = label.getAttribute('for');
        expect(forAttr).toBeTruthy();
        expect(el.querySelector(`#${forAttr}`)).toBeTruthy();
      });
    });

    it('has loading spinner with role="status"', () => {
      component.form.patchValue({
        priority: 'HIGH',
        type: 'EMAIL_SENDING',
        payload: 'test'
      });
      fixture.detectChanges();

      component.submitting.set(true);
      fixture.detectChanges();

      const spinner = fixture.nativeElement.querySelector('.spinner-border');
      expect(spinner.getAttribute('role')).toBe('status');
    });
  });
});
