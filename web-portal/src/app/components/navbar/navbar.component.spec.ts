import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NavbarComponent } from './navbar.component';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavbarComponent, RouterTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('navigation links', () => {
    it('does not render any nav links', () => {
      const el = fixture.nativeElement;
      const links = el.querySelectorAll('a.nav-link');
      expect(links.length).toBe(0);
    });

    it('does not render Create Job link', () => {
      const el = fixture.nativeElement;
      const links = el.querySelectorAll('a.nav-link');
      const linkTexts = Array.from(links).map((l: any) => l.textContent.trim());
      expect(linkTexts).not.toContain('Create Job');
    });

    it('does not render link to /jobs/create', () => {
      const el = fixture.nativeElement;
      const createLink = el.querySelector('a[href="/jobs/create"]');
      expect(createLink).toBeFalsy();
    });

    it('does not render a "Jobs" link (AC14)', () => {
      const el = fixture.nativeElement;
      const links = el.querySelectorAll('a.nav-link');
      const linkTexts = Array.from(links).map((l: any) => l.textContent.trim());
      expect(linkTexts).not.toContain('Jobs');
    });
  });

  describe('brand', () => {
    it('renders the brand name', () => {
      const el = fixture.nativeElement;
      const brand = el.querySelector('.navbar-brand');
      expect(brand).toBeTruthy();
      expect(brand.textContent).toContain('Job Queue');
    });

    it('links brand to root', () => {
      const el = fixture.nativeElement;
      const brand = el.querySelector('.navbar-brand');
      expect(brand.getAttribute('href')).toBe('/');
    });
  });
});
