import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface Alert {
  message: string;
  type: 'success' | 'danger' | 'warning' | 'info';
}

@Injectable({ providedIn: 'root' })
export class AlertService {
  private alertSubject = new Subject<Alert>();
  alert$ = this.alertSubject.asObservable();
  private timeoutId: any;

  show(message: string, type: Alert['type'] = 'info', duration = 5000) {
    clearTimeout(this.timeoutId);
    this.alertSubject.next({ message, type });
    if (duration > 0) {
      this.timeoutId = setTimeout(() => this.clear(), duration);
    }
  }

  success(message: string) { this.show(message, 'success'); }
  error(message: string) { this.show(message, 'danger', 8000); }
  warning(message: string) { this.show(message, 'warning'); }

  clear() {
    clearTimeout(this.timeoutId);
    this.alertSubject.next({ message: '', type: 'info' });
  }
}
