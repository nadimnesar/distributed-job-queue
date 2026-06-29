import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';
import { AlertComponent } from './components/alert/alert.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent, AlertComponent],
  template: `
    <app-navbar />
    <app-alert />
    <main id="main-content" class="container py-4">
      <router-outlet />
    </main>
  `,
  styles: []
})
export class AppComponent {}
