import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CommonResponse } from '../models/api.model';
import { JobsSummary, QueueMetrics } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/v1/dashboard`;

  getJobSummary(): Observable<CommonResponse<JobsSummary>> {
    return this.http.get<CommonResponse<JobsSummary>>(`${this.base}/jobs/summary`);
  }

  getQueueMetrics(): Observable<CommonResponse<QueueMetrics>> {
    return this.http.get<CommonResponse<QueueMetrics>>(`${this.base}/queue-metrics`);
  }
}
