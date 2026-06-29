import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { routes } from './app.routes';

describe('App Routes', () => {
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule.withRoutes(routes)]
    });
    router = TestBed.inject(Router);
  });

  it('should have 3 routes', () => {
    expect(routes.length).toBe(3);
  });

  describe('dashboard route', () => {
    it('maps "" to DashboardComponent', () => {
      const route = routes.find(r => r.path === '');
      expect(route).toBeTruthy();
      expect(route?.loadComponent).toBeDefined();
    });

    it('uses lazy loading', () => {
      const route = routes.find(r => r.path === '');
      expect(route?.loadComponent).toBeDefined();
    });
  });

  describe('job list route', () => {
    it('maps "jobs" to JobListComponent', () => {
      const route = routes.find(r => r.path === 'jobs');
      expect(route).toBeTruthy();
      expect(route?.loadComponent).toBeDefined();
    });
  });

  describe('job detail route', () => {
    it('maps "jobs/:id" to JobDetailComponent', () => {
      const route = routes.find(r => r.path === 'jobs/:id');
      expect(route).toBeTruthy();
      expect(route?.loadComponent).toBeDefined();
    });

    it('has :id as a route param', () => {
      const route = routes.find(r => r.path === 'jobs/:id');
      expect(route?.path).toContain(':id');
    });
  });

});
