import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-alert',
  imports: [CommonModule],
  template: `
    @if (message) {
      <div class="container mt-3">
        <div class="alert alert-{{ type }} alert-dismissible fade show" role="alert">
          {{ message }}
          <button type="button" class="btn-close" (click)="dismiss()" aria-label="Close"></button>
        </div>
      </div>
    }
  `,
  styles: []
})
export class AlertComponent implements OnInit, OnDestroy {
  message = '';
  type = 'info';
  private sub!: Subscription;

  constructor(private alertService: AlertService) {}

  ngOnInit() {
    this.sub = this.alertService.alert$.subscribe(alert => {
      this.message = alert.message;
      this.type = alert.type;
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  dismiss() {
    this.alertService.clear();
  }
}
