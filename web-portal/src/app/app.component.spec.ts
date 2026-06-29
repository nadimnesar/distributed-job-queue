import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { AlertService } from './services/alert.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent, RouterTestingModule],
      providers: [
        AlertService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it('renders the navbar', () => {
    const el = fixture.nativeElement;
    const navbar = el.querySelector('app-navbar');
    expect(navbar).toBeTruthy();
  });

  it('renders the alert component', () => {
    const el = fixture.nativeElement;
    const alert = el.querySelector('app-alert');
    expect(alert).toBeTruthy();
  });

  it('renders a main content area with id="main-content"', () => {
    const el = fixture.nativeElement;
    const main = el.querySelector('#main-content');
    expect(main).toBeTruthy();
    expect(main.tagName.toLowerCase()).toBe('main');
  });

  it('renders router-outlet inside main', () => {
    const el = fixture.nativeElement;
    const main = el.querySelector('#main-content');
    const outlet = main.querySelector('router-outlet');
    expect(outlet).toBeTruthy();
  });
});
